package com.voicereminder.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service — exists ONLY to:
 *  1. Show the persistent "Listening…" notification
 *  2. Declare microphone foreground use to Android 14+ (required or OS kills mic access)
 *
 * The actual SpeechRecognizer wake-word loop runs in VoiceSessionManager
 * (the app's main process), NOT here. Running SpeechRecognizer inside a
 * Service causes silent failures on Android 10+ Samsung devices.
 */
class WakeWordService : Service() {

    companion object {
        const val CHANNEL_ID      = "WakeWordServiceChannel"
        const val NOTIFICATION_ID = 1001

        // Kept for broadcast compatibility — no longer used for control
        const val ACTION_WAKE_WORD_DETECTED = "com.voicereminder.WAKE_WORD_DETECTED"
        const val ACTION_START  = "com.voicereminder.START"
        const val ACTION_PAUSE  = "com.voicereminder.PAUSE"
        const val ACTION_RESUME = "com.voicereminder.RESUME"

        fun pauseIntent(context: Context)  = Intent(context, WakeWordService::class.java).apply { action = ACTION_PAUSE }
        fun resumeIntent(context: Context) = Intent(context, WakeWordService::class.java).apply { action = ACTION_RESUME }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Nothing to do — engine is in VoiceSessionManager
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoiceReminder")
            .setContentText("Say 'Hey' or 'Hello' to activate…")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Voice Assistant", NotificationManager.IMPORTANCE_LOW)
                .apply { setSound(null, null) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }
}
