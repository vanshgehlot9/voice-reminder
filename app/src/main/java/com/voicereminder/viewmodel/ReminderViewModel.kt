package com.voicereminder.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voicereminder.alarm.AlarmScheduler
import com.voicereminder.data.ReminderDatabase
import com.voicereminder.data.ReminderEntity
import com.voicereminder.network.MobileCommandResponse
import com.voicereminder.network.MobileTextCommandRequest
import com.voicereminder.network.VoiceboxApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.util.*

private const val TAG = "VoiceboxVM"

// ============================================================
// UI State
// ============================================================

sealed class RecordingState {
    object Idle       : RecordingState()
    object Activating : RecordingState()
    object Recording  : RecordingState()
    object Processing : RecordingState()
    data class Success(val response: MobileCommandResponse) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

enum class MessageSender { USER, ASSISTANT }

data class ChatMessage(
    val id       : String = UUID.randomUUID().toString(),
    val sender   : MessageSender,
    val text     : String,
    val isError  : Boolean = false
)

data class HomeUiState(
    val recordingState  : RecordingState   = RecordingState.Idle,
    val liveTranscript  : String           = "",
    val voiceRmsDb      : Float            = 0f,
    val recordingStartedAtMs: Long?        = null,
    val serverReachable : Boolean?         = null,
    val pendingCount    : Int              = 0,
    val messages        : List<ChatMessage> = emptyList(),
    val isAutoMode      : Boolean          = true  // Siri-like continuous mode
)

// ============================================================
// ViewModel — Siri/Gemini-style continuous voice assistant
// ============================================================

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val context : Context get() = getApplication()
    private val db      = ReminderDatabase.getInstance(context)
    private val dao     = db.reminderDao()
    private val api     get() = VoiceboxApiClient.get()  // always fresh after login

    private var tts             : TextToSpeech?    = null
    // Replaced with true Voice architecture
    private var voiceSessionManager: com.voicereminder.voice.VoiceSessionManager? = null

    // Stable StateFlow that Compose collects — NEVER replaced, only its VALUE changes.
    // This fixes the bug where Compose locked onto a dead dummy flow.
    private val _voiceUiState = MutableStateFlow(com.voicereminder.voice.VoiceUiState())
    val voiceUiState: StateFlow<com.voicereminder.voice.VoiceUiState> = _voiceUiState.asStateFlow()
    private var processingJob   : Job?              = null

    // Prevents double-submit race between onResults and stopRecordingAndProcess
    @Volatile private var finalResultReceived = false
    // Prevents auto-restart loop when user explicitly cancels
    @Volatile private var userCancelled       = false
    // Whether TTS finished speaking (we wait before auto-listening)
    @Volatile private var ttsSpeaking         = false

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val pendingReminders: StateFlow<List<ReminderEntity>> =
        dao.observePending().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allReminders: StateFlow<List<ReminderEntity>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        initTts()
        checkServerHealth()
        observePendingCount()
        // VoiceSessionManager is started only after RECORD_AUDIO permission is granted.
        // Call onMicPermissionGranted() from the Activity once the permission dialog is accepted.
    }

    /** Call this from MainActivity once RECORD_AUDIO permission is confirmed. */
    fun onMicPermissionGranted() {
        if (voiceSessionManager != null) return // already started
        voiceSessionManager = com.voicereminder.voice.VoiceSessionManager(
            context = getApplication(),
            coroutineScope = viewModelScope,
            commandProcessor = { text -> submitVoiceCommand(text) }
        )
        // Pipe VoiceSessionManager states into both StateFlows so Compose and all screens see live updates
        viewModelScope.launch {
            voiceSessionManager!!.uiState.collect { state ->
                _voiceUiState.value = state
                _uiState.update { home ->
                    when (state.state) {
                        com.voicereminder.voice.VoiceState.IDLE,
                        com.voicereminder.voice.VoiceState.STARTING_WAKE_WORD,
                        com.voicereminder.voice.VoiceState.LISTENING_FOR_WAKE_WORD ->
                            home.copy(
                                recordingState = if (home.recordingState is RecordingState.Success || home.recordingState is RecordingState.Error) home.recordingState else RecordingState.Idle,
                                liveTranscript = "",
                                voiceRmsDb = 0f
                            )
                        com.voicereminder.voice.VoiceState.WAKE_WORD_DETECTED,
                        com.voicereminder.voice.VoiceState.ACTIVATING ->
                            home.copy(
                                recordingState = RecordingState.Activating,
                                liveTranscript = "",
                                voiceRmsDb = 0f
                            )
                        com.voicereminder.voice.VoiceState.LISTENING_FOR_COMMAND ->
                            home.copy(
                                recordingState = RecordingState.Recording,
                                liveTranscript = state.transcript,
                                voiceRmsDb = state.waveformAmplitude * 10f,
                                recordingStartedAtMs = home.recordingStartedAtMs ?: System.currentTimeMillis()
                            )
                        com.voicereminder.voice.VoiceState.PROCESSING_COMMAND ->
                            home.copy(
                                recordingState = RecordingState.Processing,
                                liveTranscript = state.transcript
                            )
                        com.voicereminder.voice.VoiceState.SPEAKING_RESPONSE ->
                            home
                        com.voicereminder.voice.VoiceState.ERROR ->
                            home.copy(
                                recordingState = RecordingState.Error(state.error ?: "Error")
                            )
                        else -> home
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // TTS — waits for init then auto-starts listening
    // ------------------------------------------------------------------

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.getDefault())
                }
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.w(TAG, "TTS init failed")
            }
        }
    }

    /**
     * Speak [text] via TTS on the media stream
     */
    private fun speakThenListen(text: String, isFirstStart: Boolean = false) {
        if (tts == null) return
        ttsSpeaking = true
        val utteranceId = "vb_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(id: String?) { ttsSpeaking = true }
            override fun onDone(id: String?) {
                ttsSpeaking = false
                viewModelScope.launch(Dispatchers.Main) {
                    voiceSessionManager?.onTtsFinished()
                }
            }
            override fun onError(id: String?) {
                ttsSpeaking = false
                viewModelScope.launch(Dispatchers.Main) {
                    voiceSessionManager?.onTtsFinished()
                }
            }
        })
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    
    fun startRecording() {
        voiceSessionManager?.manualTrigger()
    }

    fun stopRecordingAndProcess() {
        // VoiceSessionManager handles this via timeout or results
    }

    fun cancelRecording() {
        voiceSessionManager?.cancel()
    }

    fun dismissState() {
        voiceSessionManager?.cancel()
    }

    // ------------------------------------------------------------------
    // Network calls
    // ------------------------------------------------------------------

    fun submitManualText(text: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _uiState.update { it.copy(recordingState = RecordingState.Processing) }
            submitVoiceCommand(text)
        }
    }

    fun submitVoiceCommand(transcript: String) {
        addUserMessage(transcript)
        processingJob?.cancel()
        processingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val timezone = TimeZone.getDefault().id
                val response = api.processTextCommand(
                    MobileTextCommandRequest(transcript, timezone, "1.7B")
                )

                if (!response.isSuccessful) {
                    val msg = when (response.code()) {
                        401  -> "Session expired. Please log in again."
                        404  -> "Backend endpoint not found. Is the server running?"
                        500  -> "Server error. The backend may be busy."
                        else -> "Server returned ${response.code()}."
                    }
                    addAssistantMessage(msg, isError = true)
                    voiceSessionManager?.completeCommand(com.voicereminder.voice.VoiceCommandResult(false, "error", msg))
                    _uiState.update { it.copy(recordingState = RecordingState.Idle, liveTranscript = "") }
                    withContext(Dispatchers.Main) {
                        speakThenListen("Sorry, $msg")
                    }
                    return@launch
                }

                val body = response.body()
                if (body == null) {
                    val msg = "I got an empty response. Please try again."
                    addAssistantMessage(msg, isError = true)
                    voiceSessionManager?.completeCommand(com.voicereminder.voice.VoiceCommandResult(false, "error", msg))
                    _uiState.update { it.copy(recordingState = RecordingState.Idle, liveTranscript = "") }
                    withContext(Dispatchers.Main) { speakThenListen(msg) }
                    return@launch
                }

                val reply = body.replyText.ifBlank { "Got it." }
                addAssistantMessage(reply)
                voiceSessionManager?.completeCommand(com.voicereminder.voice.VoiceCommandResult(true, body.intent ?: "", reply))
                _uiState.update { it.copy(recordingState = RecordingState.Success(body), liveTranscript = "") }

                if (body.success && body.intent == "create_reminder" && body.scheduledAt != null) {
                    saveAndScheduleReminder(body, timezone)
                }

                // Handle Device Actions (open_app, call, play_music, web_search)
                when (body.intent) {
                    "open_app" -> {
                        withContext(Dispatchers.Main) {
                            com.voicereminder.util.DeviceActionHandler.openApp(context, body.actionTarget ?: "google")
                        }
                    }
                    "call" -> {
                        withContext(Dispatchers.Main) {
                            com.voicereminder.util.DeviceActionHandler.makeCall(context, body.actionTarget ?: "")
                        }
                    }
                    "play_music" -> {
                        withContext(Dispatchers.Main) {
                            com.voicereminder.util.DeviceActionHandler.playMusic(context, body.actionTarget ?: "music")
                        }
                    }
                    "web_search" -> {
                        withContext(Dispatchers.Main) {
                            com.voicereminder.util.DeviceActionHandler.searchWeb(context, body.actionTarget ?: "")
                        }
                    }
                    "create_document", "create_note" -> {
                        val docTitle = body.documentTitle ?: body.task ?: "Note"
                        val docContent = body.documentContent ?: body.transcript ?: ""
                        // 1. Save in Room DB so it immediately appears in Notes & Documents
                        val noteEntity = ReminderEntity(
                            id = UUID.randomUUID().toString(),
                            task = docTitle,
                            scheduledAt = 0L,
                            scheduledAtIso = "",
                            timezone = timezone,
                            transcript = docContent,
                            replyText = reply,
                            isCompleted = false,
                            createdAt = System.currentTimeMillis()
                        )
                        dao.insert(noteEntity)

                        // 2. Generate PDF document automatically
                        try {
                            val pdfFile = com.voicereminder.util.PdfGenerator.generateDocumentPdf(context, docTitle, docContent)
                            Log.i(TAG, "Generated PDF document: ${pdfFile?.absolutePath}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to generate PDF: ${e.message}")
                        }
                    }
                }

                withContext(Dispatchers.Main) { speakThenListen(reply) }

            } catch (e: Exception) {
                Log.e(TAG, "submitVoiceCommand error: ${e.message}", e)
                val msg = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "I can't reach the server. Make sure your phone and laptop are on the same WiFi."
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "The server took too long to respond. Try again."
                    else -> "Connection error. Please check the server."
                }
                addAssistantMessage(msg, isError = true)
                voiceSessionManager?.completeCommand(com.voicereminder.voice.VoiceCommandResult(false, "error", msg))
                _uiState.update { it.copy(recordingState = RecordingState.Idle, liveTranscript = "") }
                withContext(Dispatchers.Main) {
                    speakThenListen("Sorry, $msg")
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Health check
    // ------------------------------------------------------------------

    fun checkServerHealth() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = api.healthCheck()
                _uiState.update { it.copy(serverReachable = res.isSuccessful) }
            } catch (e: Exception) {
                _uiState.update { it.copy(serverReachable = false) }
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun addUserMessage(text: String) {
        _uiState.update { s ->
            s.copy(messages = s.messages + ChatMessage(sender = MessageSender.USER, text = text))
        }
    }

    private fun addAssistantMessage(text: String, isError: Boolean = false) {
        _uiState.update { s ->
            s.copy(messages = s.messages + ChatMessage(sender = MessageSender.ASSISTANT, text = text, isError = isError))
        }
    }

    private suspend fun saveAndScheduleReminder(body: MobileCommandResponse, timezone: String) {
        try {
            val millis = OffsetDateTime.parse(body.scheduledAt).toInstant().toEpochMilli()
            val id     = UUID.randomUUID().toString()
            val entity = ReminderEntity(
                id             = id,
                task           = body.task ?: "Reminder",
                scheduledAt    = millis,
                scheduledAtIso = body.scheduledAt ?: "",
                timezone       = timezone,
                transcript     = body.transcript ?: "",
                replyText      = body.replyText,
            )
            dao.insert(entity)
            AlarmScheduler.schedule(context, id, entity.task, millis)
            Log.i(TAG, "Saved reminder: '${entity.task}' at ${entity.scheduledAtIso}")
        } catch (e: Exception) {
            Log.e(TAG, "saveAndScheduleReminder: ${e.message}", e)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            AlarmScheduler.cancel(context, reminder.id)
            dao.deleteById(reminder.id)
        }
    }

    fun updateReminder(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(reminder)
            if (!reminder.isCompleted) {
                AlarmScheduler.schedule(context, reminder.id, reminder.task, reminder.scheduledAt)
            }
        }
    }

    fun markReminderCompleted(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            AlarmScheduler.cancel(context, reminder.id)
            dao.update(reminder.copy(isCompleted = true))
        }
    }

    fun snoozeReminder(reminder: ReminderEntity, snoozeMinutes: Long = 10L) {
        viewModelScope.launch(Dispatchers.IO) {
            val newScheduledAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
            AlarmScheduler.cancel(context, reminder.id)
            dao.update(
                reminder.copy(
                    scheduledAt = newScheduledAt,
                    scheduledAtIso = java.time.Instant.ofEpochMilli(newScheduledAt).atOffset(java.time.ZoneOffset.UTC).toString(),
                    isCompleted = false,
                )
            )
            AlarmScheduler.schedule(context, reminder.id, reminder.task, newScheduledAt)
        }
    }

    fun rescheduleReminder(reminder: ReminderEntity, scheduledAtMillis: Long = System.currentTimeMillis() + 60 * 60 * 1000L) {
        viewModelScope.launch(Dispatchers.IO) {
            AlarmScheduler.cancel(context, reminder.id)
            dao.update(
                reminder.copy(
                    scheduledAt = scheduledAtMillis,
                    scheduledAtIso = java.time.Instant.ofEpochMilli(scheduledAtMillis).atOffset(java.time.ZoneOffset.UTC).toString(),
                    isCompleted = false,
                )
            )
            AlarmScheduler.schedule(context, reminder.id, reminder.task, scheduledAtMillis)
        }
    }

    private fun observePendingCount() {
        viewModelScope.launch {
            dao.observePending().collect { list ->
                _uiState.update { it.copy(pendingCount = list.size) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        processingJob?.cancel()
        tts?.stop()
        tts?.shutdown()
        voiceSessionManager?.destroy()
    }
}
