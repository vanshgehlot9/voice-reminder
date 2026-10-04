package com.voicereminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.voicereminder.data.ReminderDatabase
import com.voicereminder.notification.NotificationHelper
import com.voicereminder.notification.ReminderSpeakerService
import com.voicereminder.notification.EXTRA_SPEAK_TEXT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.TimeZone

private const val TAG = "VoiceReminder"
private const val EXTRA_REMINDER_ID = "reminder_id"
private const val EXTRA_TASK = "task"
private const val ACTION_REMINDER = "com.voicereminder.ACTION_REMINDER"

// ============================================================
// AlarmScheduler — schedules / cancels alarms via AlarmManager
// ============================================================

/**
 * Schedules exact alarms for reminders.
 *
 * Low-end device notes:
 * ─────────────────────
 * • Uses setExactAndAllowWhileIdle() which fires even in Doze mode.
 *   This is more reliable than setExact() on battery-optimised cheap devices.
 * • On Android 12+ (API 31), SCHEDULE_EXACT_ALARM permission is required.
 *   The app guides the user to grant it on first launch.
 */
object AlarmScheduler {

    fun schedule(context: Context, reminderId: String, task: String, scheduledAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = buildReminderIntent(context, reminderId, task)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms — user must grant permission in Settings")
                // Fallback: schedule as inexact (may fire up to 15 min late on Doze devices)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    scheduledAtMillis,
                    intent,
                )
                return
            }
        }

        // Fires exactly at scheduledAtMillis even in Doze mode
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            scheduledAtMillis,
            intent,
        )

        Log.i(TAG, "Alarm scheduled: id=$reminderId at $scheduledAtMillis")
    }

    fun cancel(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = buildReminderIntent(context, reminderId, "")
        alarmManager.cancel(intent)
        Log.i(TAG, "Alarm cancelled: id=$reminderId")
    }

    private fun buildReminderIntent(context: Context, reminderId: String, task: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TASK, task)
        }
        // FLAG_UPDATE_CURRENT: replaces any existing PendingIntent with same request code
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        // Use reminderId hashCode as requestCode so each reminder gets a unique PendingIntent
        return PendingIntent.getBroadcast(context, reminderId.hashCode(), intent, flags)
    }
}

// ============================================================
// ReminderReceiver — fires when alarm triggers
// ============================================================

/**
 * BroadcastReceiver that handles:
 *  1. Scheduled reminder alarms (ACTION_REMINDER)
 *  2. Device reboot (BOOT_COMPLETED) — re-schedules all pending alarms
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {

            ACTION_REMINDER -> {
                val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
                val task = intent.getStringExtra(EXTRA_TASK) ?: "Reminder"

                Log.i(TAG, "Alarm fired: id=$reminderId task=$task")

                // 1. Show heads-up notification
                NotificationHelper.showReminderNotification(context, reminderId, task)

                // 2. Speak the reminder aloud via TTS
                val spokenText = "Hey! Your reminder: $task"
                val speakIntent = Intent(context, ReminderSpeakerService::class.java).apply {
                    putExtra(EXTRA_SPEAK_TEXT, spokenText)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(speakIntent)
                } else {
                    context.startService(speakIntent)
                }

                // 3. Mark reminder completed in Room (async)
                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    try {
                        val db = ReminderDatabase.getInstance(context)
                        db.reminderDao().markCompleted(reminderId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to mark reminder completed: ${e.message}")
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED" -> {
                // Re-schedule all pending alarms after reboot
                Log.i(TAG, "Boot completed — re-scheduling pending alarms")
                rescheduleAfterBoot(context)
            }
        }
    }

    private fun rescheduleAfterBoot(context: Context) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val db = ReminderDatabase.getInstance(context)
                val pending = db.reminderDao().getPendingAfter(System.currentTimeMillis())
                Log.i(TAG, "Re-scheduling ${pending.size} reminder(s) after boot")
                pending.forEach { reminder ->
                    AlarmScheduler.schedule(
                        context,
                        reminder.id,
                        reminder.task,
                        reminder.scheduledAt,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-schedule alarms after boot: ${e.message}")
            }
        }
    }
}
