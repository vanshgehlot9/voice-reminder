package com.voicereminder.voice

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/**
 * Wake-word engine that runs in the caller's process (not a Service).
 * This is critical — SpeechRecognizer silently fails when created from a
 * background Service context on Android 10+ Samsung devices.
 *
 * Must be started and stopped on the MAIN thread.
 */
class SpeechRecognizerWakeWordEngine(private val context: Context) : WakeWordEngine {

    private val TAG = "WakeWordEngine"
    private val mainHandler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var onWakeWordDetectedCallback: ((transcript: String?) -> Unit)? = null
    @Volatile private var isActive = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Watchdog: restart if recognizer silently dies (no callback for 15s)
    private val watchdogRunnable = Runnable {
        if (isActive) {
            Log.w(TAG, "Watchdog fired — restarting dead recognizer")
            restartCycle(300L)
        }
    }
    private val safeUnmuteRunnable = Runnable { unmute() }

    // ── Public API ─────────────────────────────────────────────────────────────

    override fun start(onWakeWordDetected: (transcript: String?) -> Unit, onError: (String) -> Unit) {
        if (isActive) {
            Log.d(TAG, "Already active — ignoring start()")
            return
        }
        Log.d(TAG, "start()")
        onWakeWordDetectedCallback = onWakeWordDetected
        isActive = true
        mainHandler.post { startCycle() }
    }

    override fun stop() {
        Log.d(TAG, "stop()")
        isActive = false
        mainHandler.post {
            cancelWatchdog()
            tearDownRecognizer()
            unmute()
        }
    }

    override fun destroy() = stop()

    // ── Internal cycle ─────────────────────────────────────────────────────────

    private fun startCycle() {
        if (!isActive) return
        check(Looper.myLooper() == Looper.getMainLooper()) { "Must run on main thread" }

        tearDownRecognizer()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available!")
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { sr ->
            sr.setRecognitionListener(makeListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // Keep listening as long as possible before timing out
            putExtra("android.speech.extra.DICTATION_MODE", true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 8000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 8000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
        }

        mute()
        recognizer!!.startListening(intent)
        Log.d(TAG, "startListening() called — armed watchdog")
        armWatchdog()
    }

    private fun makeListener() = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech ✓")
            unmute()              // beep window is over
            armWatchdog()         // reset watchdog — we know we're alive
        }

        override fun onRmsChanged(rmsdB: Float) {
            armWatchdog()         // frequent proof-of-life
        }

        override fun onBeginningOfSpeech() {
            cancelWatchdog()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            Log.d(TAG, "Partial: $text")
            val lower = text.lowercase()
            if (isWakeWord(lower)) {
                // If it's ONLY a greeting/wake word, trigger immediately
                val cleaned = lower.replace(Regex("(?i)^(hey voicereminder|hey voicebox|hey assistant|hey jarvis|hey siri|hey google|ok google|okay google|hey|hello|hi|ok voice|okay|voicebox|voice box|voice reminder|voicereminder|suno|batao)\\s*"), "").trim()
                if (cleaned.isBlank() || cleaned in listOf("hello", "hey", "hi", "siri", "jarvis", "alexa", "ok", "okay", "suno", "batao")) {
                    Log.i(TAG, "⚡ Wake word only (partial): $text")
                    cancelWatchdog()
                    tearDownRecognizer()
                    onWakeWordDetectedCallback?.invoke(text)
                }
            }
        }

        override fun onResults(results: Bundle?) {
            cancelWatchdog()
            val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.d(TAG, "Results: ${candidates?.joinToString(" | ")}")
            val matching = candidates?.firstOrNull { isWakeWord(it.lowercase()) }
            if (matching != null) {
                Log.i(TAG, "⚡ Wake word (final): $matching")
                tearDownRecognizer()
                onWakeWordDetectedCallback?.invoke(matching)
            } else {
                restartCycle(150L)  // loop immediately
            }
        }

        override fun onError(error: Int) {
            cancelWatchdog()
            val name = errorName(error)
            Log.w(TAG, "onError: $name ($error)")
            val delay = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 100L
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 800L
                SpeechRecognizer.ERROR_SERVER         -> 2000L
                SpeechRecognizer.ERROR_CLIENT         -> 600L
                else -> 500L
            }
            restartCycle(delay)
        }

        override fun onEndOfSpeech() { Log.d(TAG, "onEndOfSpeech") }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun isWakeWord(text: String): Boolean {
        val t = text.lowercase()
        val triggers = listOf(
            "hey", "hello", "hi", "siri", "jarvis", "alexa",
            "voicebox", "voice box", "voice reminder", "voicereminder",
            "ok voice", "ok google", "okay", "listen", "suno", "batao",
            "remind", "reminder", "set a reminder", "yaad dilao", "yaad dilana", "alarm", "schedule"
        )
        return triggers.any { t.contains(it) }
    }

    private fun restartCycle(delayMs: Long) {
        tearDownRecognizer()
        if (isActive) mainHandler.postDelayed({ startCycle() }, delayMs)
    }

    private fun tearDownRecognizer() {
        try { recognizer?.cancel() } catch (_: Exception) {}
        try { recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
    }

    private fun armWatchdog() {
        mainHandler.removeCallbacks(watchdogRunnable)
        mainHandler.postDelayed(watchdogRunnable, 15_000L)
    }

    private fun cancelWatchdog() = mainHandler.removeCallbacks(watchdogRunnable)

    private fun mute() {
        try {
            mainHandler.removeCallbacks(safeUnmuteRunnable)
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING,   AudioManager.ADJUST_MUTE, 0)
            mainHandler.postDelayed(safeUnmuteRunnable, 700L)
        } catch (_: Exception) {}
    }

    private fun unmute() {
        try {
            mainHandler.removeCallbacks(safeUnmuteRunnable)
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING,   AudioManager.ADJUST_UNMUTE, 0)
        } catch (_: Exception) {}
    }

    private fun errorName(code: Int) = when (code) {
        SpeechRecognizer.ERROR_AUDIO                  -> "AUDIO"
        SpeechRecognizer.ERROR_CLIENT                 -> "CLIENT"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "NO_PERMISSIONS"
        SpeechRecognizer.ERROR_NETWORK                -> "NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT        -> "NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH               -> "NO_MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY        -> "BUSY"
        SpeechRecognizer.ERROR_SERVER                 -> "SERVER"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT         -> "SPEECH_TIMEOUT"
        else -> "UNKNOWN($code)"
    }
}
