package com.voicereminder.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.voicereminder.MainActivity
import com.voicereminder.R

/**
 * Manages notification channels and shows reminder notifications.
 *
 * Low-end device notes:
 * ─────────────────────
 * • The notification is the PRIMARY reliable mechanism for reminders.
 * • TTS is SECONDARY (shown only if the app is visible / user enabled voice mode).
 * • On low-RAM devices the app process may have been killed — the notification
 *   fires from the BroadcastReceiver regardless of app state.
 */
object NotificationHelper {

    const val CHANNEL_ID = "voicereminder_channel"
    const val CHANNEL_NAME = "Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH, // Heads-up notification
            ).apply {
                description = "Voice reminder alerts"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(context: Context, reminderId: String, task: String) {
        // Tap notification → open the app
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("reminder_id", reminderId)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            tapIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ Reminder")
            .setContentText(task)
            .setStyle(NotificationCompat.BigTextStyle().bigText(task))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Use reminderId hashCode as notificationId — each reminder gets its own notification
        manager.notify(reminderId.hashCode(), notification)
    }
}
