package com.voicereminder.notification

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

private const val TAG = "VoiceReminder"
const val EXTRA_SPEAK_TEXT = "speak_text"
private const val TTS_UTTERANCE_ID = "reminder_speak"
private const val FOREGROUND_NOTIF_ID = 9901

/**
 * A short-lived foreground Service that:
 *  1. Initialises Android TTS
 *  2. Speaks the reminder text aloud (e.g. "Hey! Your reminder: Take medicine.")
 *  3. Stops itself as soon as the speech finishes (or fails)
 *
 * Running as a foreground service lets us speak even when the screen is off /
 * the app process was previously killed by Doze mode.
 */
class ReminderSpeakerService : Service(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var textToSpeak: String = "Hey! You have a reminder."

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(applicationContext, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must call startForeground() before anything else on API 26+
        startForeground(FOREGROUND_NOTIF_ID, buildSilentNotification())

        textToSpeak = intent?.getStringExtra(EXTRA_SPEAK_TEXT)
            ?.takeIf { it.isNotBlank() }
            ?: "Hey! You have a reminder."

        // TTS may already be ready if the service was reused; speak immediately.
        // Otherwise onInit() will pick it up.
        tts?.let { engine ->
            if (engine.isSpeaking) return@let
            speakNow(engine)
        }

        return START_NOT_STICKY   // Don't restart automatically — one-shot
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    // ── TTS init callback ──────────────────────────────────────────────────

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                // Prefer English; fall back gracefully if unsupported
                val result = engine.setLanguage(Locale.ENGLISH)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.w(TAG, "TTS: English not supported; using device default")
                }
                // Slightly slower rate + louder pitch so it sounds like an assistant
                engine.setSpeechRate(0.90f)
                engine.setPitch(1.05f)

                // Route through alarm/notification stream so DND / volume is respected
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val params = android.os.Bundle()
                    params.putInt(
                        TextToSpeech.Engine.KEY_PARAM_STREAM,
                        AudioManager.STREAM_ALARM
                    )
                    // no direct setAudioAttributes in TTS pre-P; use bundle param
                    engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, TTS_UTTERANCE_ID)
                } else {
                    @Suppress("DEPRECATION")
                    engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null)
                }

                // Stop service when done
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        Log.i(TAG, "TTS finished — stopping ReminderSpeakerService")
                        stopSelf()
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "TTS error — stopping ReminderSpeakerService")
                        stopSelf()
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e(TAG, "TTS error ($errorCode) — stopping ReminderSpeakerService")
                        stopSelf()
                    }
                })
            }
        } else {
            Log.e(TAG, "TTS initialisation failed with status=$status")
            stopSelf()
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun speakNow(engine: TextToSpeech) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val params = android.os.Bundle()
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
            engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, TTS_UTTERANCE_ID)
        } else {
            @Suppress("DEPRECATION")
            engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    /**
     * A silent, minimal notification required by Android to keep the foreground
     * service alive. The user sees the real reminder notification separately.
     */
    private fun buildSilentNotification(): Notification {
        // Reuse the existing channel (already created in NotificationHelper)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("VoiceReminder")
                .setContentText("Speaking your reminder…")
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("VoiceReminder")
                .setContentText("Speaking your reminder…")
                .setPriority(Notification.PRIORITY_LOW)
                .setOngoing(true)
                .build()
        }
    }
}
