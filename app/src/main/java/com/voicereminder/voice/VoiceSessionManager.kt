package com.voicereminder.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Central voice state machine.
 *
 * Architecture (fixed):
 *  - The wake-word SpeechRecognizer runs HERE, in the app process (not a Service).
 *    Running it inside a Service causes silent failures on Android 10+ Samsung devices.
 *  - WakeWordService is kept alive only to show the foreground notification and
 *    declare microphone use to the OS (required for Android 14+).
 *  - Command SpeechRecognizer also runs here, with a 1.5 s gap after stopping the
 *    wake-word loop so the mic hardware fully releases.
 */
class VoiceSessionManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val commandProcessor: (String) -> Unit
) {
    private val TAG = "VoiceSessionManager"

    companion object {
        @Volatile var instance: VoiceSessionManager? = null
        @Volatile var isAppInForeground: Boolean = false
    }

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    // ── Wake-word engine (runs in this process) ──────────────────────────────
    private var wakeWordEngine: SpeechRecognizerWakeWordEngine? = null

    // ── Command recognizer ────────────────────────────────────────────────────
    private var commandRecognizer: SpeechRecognizer? = null

    private var timeoutJob: Job? = null
    private var cooldownJob: Job? = null

    init {
        instance = this
        // Start the foreground Service so Android sees mic usage properly
        startNotificationService()
        // Start wake-word loop immediately
        startWakeWordLoop()
    }

    // ── Wake-word loop ────────────────────────────────────────────────────────

    private fun startWakeWordLoop() {
        val currentState = _uiState.value.state
        if (currentState != VoiceState.IDLE && currentState != VoiceState.ERROR) return

        _uiState.update { it.copy(state = VoiceState.LISTENING_FOR_WAKE_WORD, transcript = "", error = null) }
        Log.d(TAG, "Starting wake-word loop")

        wakeWordEngine?.stop()
        wakeWordEngine?.destroy()
        wakeWordEngine = SpeechRecognizerWakeWordEngine(context.applicationContext)
        wakeWordEngine!!.start(
            onWakeWordDetected = { transcript -> onWakeWordDetected(transcript) },
            onError = { msg -> Log.e(TAG, "Wake-word error: $msg") }
        )
    }

    private fun stopWakeWordLoop() {
        wakeWordEngine?.stop()
        wakeWordEngine?.destroy()
        wakeWordEngine = null
    }

    private fun onWakeWordDetected(spokenText: String? = null) {
        if (_uiState.value.state != VoiceState.LISTENING_FOR_WAKE_WORD) return
        Log.d(TAG, "Wake word detected! spokenText=$spokenText, isAppInForeground=$isAppInForeground")

        // If the app is in the background (user is on Home screen or another app),
        // launch the translucent floating Siri Assistant overlay!
        if (!isAppInForeground) {
            launchAssistantOverlay()
        }

        stopWakeWordLoop()

        val cleaned = spokenText?.let { cleanWakeWord(it) }?.trim()

        if (!cleaned.isNullOrBlank() && !isOnlyGreeting(cleaned)) {
            // User spoke the full command along with the wake word! (e.g. "Hey remind me to buy milk tomorrow at 5pm")
            Log.d(TAG, "Full command received with wake word: '$cleaned'")
            _uiState.update { it.copy(state = VoiceState.PROCESSING_COMMAND, transcript = cleaned) }
            commandProcessor(cleaned)
        } else {
            // User only said "Hey" or "Hello" — open the Siri listening mode!
            Log.d(TAG, "Greeting/wake word only — listening for follow-up command...")
            _uiState.update { it.copy(state = VoiceState.WAKE_WORD_DETECTED) }
            coroutineScope.launch(Dispatchers.Main) {
                delay(600)
                startCommandListening()
            }
        }
    }

    private fun cleanWakeWord(transcript: String): String {
        return transcript
            .replace(Regex("(?i)^(hey voicereminder|hey voicebox|hey assistant|hey jarvis|hey siri|hey google|ok google|okay google|hey|hello|hi|ok voice|okay|voicebox|voice box|voice reminder|voicereminder|suno|batao)\\s*"), "")
            .trim()
    }

    private fun isOnlyGreeting(text: String): Boolean {
        val t = text.lowercase().trim()
        return t.isBlank() || t in listOf("hello", "hey", "hi", "siri", "jarvis", "alexa", "ok", "okay", "suno", "batao")
    }

    private fun launchAssistantOverlay() {
        try {
            val intent = Intent(context, VoiceAssistantActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
            Log.i(TAG, "Launched VoiceAssistantActivity translucent overlay successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Could not launch VoiceAssistantActivity directly", e)
        }
    }

    // ── Manual trigger (mic button) ───────────────────────────────────────────

    fun manualTrigger() {
        stopWakeWordLoop()
        destroyCommandRecognizer()
        _uiState.update { it.copy(state = VoiceState.WAKE_WORD_DETECTED, transcript = "", error = null, response = null) }
        coroutineScope.launch(Dispatchers.Main) {
            delay(500)
            startCommandListening()
        }
    }

    // ── Command listening ─────────────────────────────────────────────────────

    private fun startCommandListening() {
        _uiState.update { it.copy(state = VoiceState.LISTENING_FOR_COMMAND, transcript = "") }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            handleError("Speech recognition not available on this device")
            return
        }

        commandRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Command recognizer ready")
                    startTimeout()
                }
                override fun onBeginningOfSpeech() { cancelTimeout() }
                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = (rmsdB / 10f).coerceIn(0f, 1f)
                    _uiState.update { it.copy(waveformAmplitude = normalized) }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        _uiState.update { it.copy(transcript = partial) }
                    }
                }
                override fun onResults(results: Bundle?) {
                    val transcript = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: ""
                    handleCommandResult(transcript)
                }
                override fun onError(error: Int) {
                    if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        returnToIdle("Timeout")
                    } else {
                        handleError("Recognition error: $error")
                    }
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            startListening(intent)
        }
    }

    // ── Command result ────────────────────────────────────────────────────────

    private fun handleCommandResult(transcript: String) {
        destroyCommandRecognizer()

        val cleaned = transcript
            .replace(Regex("(?i)^(hey voicereminder|hey voicebox|hey assistant|hey jarvis|hey siri|hey|hello|hi|ok google|okay|voicebox|voice box|voice reminder|voicereminder|suno|batao)\\s*"), "")
            .trim()

        val finalCommand = if (cleaned.isNotBlank()) cleaned else transcript.trim()

        if (finalCommand.isBlank()) {
            returnToIdle("Nothing heard")
            return
        }

        _uiState.update { it.copy(state = VoiceState.PROCESSING_COMMAND, transcript = finalCommand) }
        commandProcessor(finalCommand)
    }

    fun completeCommand(result: VoiceCommandResult) {
        _uiState.update { it.copy(state = VoiceState.SPEAKING_RESPONSE, response = result) }
        // Fallback: if TTS callback doesn't arrive within 6s, return to idle
        coroutineScope.launch(Dispatchers.Main) {
            delay(6000)
            if (_uiState.value.state == VoiceState.SPEAKING_RESPONSE) {
                returnToIdle("Fallback speaking timeout")
            }
        }
    }

    fun onTtsFinished() {
        if (_uiState.value.state == VoiceState.SPEAKING_RESPONSE) {
            coroutineScope.launch(Dispatchers.Main) {
                delay(1200) // Keep card visible for 1.2s so user can see the confirmation
                returnToIdle("TTS finished")
            }
        }
    }

    fun cancel() {
        destroyCommandRecognizer()
        returnToIdle("Cancelled")
    }

    // ── Error / idle helpers ──────────────────────────────────────────────────

    private fun handleError(message: String) {
        Log.e(TAG, "Error: $message")
        destroyCommandRecognizer()
        _uiState.update { it.copy(state = VoiceState.ERROR, error = message) }
        coroutineScope.launch {
            delay(2000)
            returnToIdle()
        }
    }

    private fun returnToIdle(reason: String = "") {
        Log.d(TAG, "Returning to IDLE${if (reason.isNotBlank()) ": $reason" else ""}")
        destroyCommandRecognizer()
        _uiState.update {
            it.copy(state = VoiceState.IDLE, transcript = "", waveformAmplitude = 0f,
                error = null, response = null)
        }
        cooldownJob?.cancel()
        cooldownJob = coroutineScope.launch(Dispatchers.Main) {
            delay(1200)       // mic release window before reopening
            startWakeWordLoop()
        }
    }

    // ── Timeout (command mode) ────────────────────────────────────────────────

    private fun startTimeout() {
        timeoutJob?.cancel()
        timeoutJob = coroutineScope.launch {
            delay(7000)
            destroyCommandRecognizer()
            returnToIdle("Timeout")
        }
    }

    private fun cancelTimeout() { timeoutJob?.cancel() }

    // ── Cleanup helpers ───────────────────────────────────────────────────────

    private fun destroyCommandRecognizer() {
        try {
            commandRecognizer?.stopListening()
            commandRecognizer?.destroy()
        } catch (_: Exception) {}
        commandRecognizer = null
    }

    fun destroy() {
        stopWakeWordLoop()
        destroyCommandRecognizer()
        timeoutJob?.cancel()
        cooldownJob?.cancel()
        // Stop the notification service
        context.stopService(Intent(context, WakeWordService::class.java))
    }

    // ── Notification service (foreground mic declaration only) ────────────────

    private fun startNotificationService() {
        val intent = Intent(context, WakeWordService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not start WakeWordService", e)
        }
    }
}
