package com.example.admission

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.FokusApplication
import com.example.MainActivity
import com.example.R
import com.example.data.database.FokusDatabase
import com.example.data.model.AdmissionReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AdmissionNotificationScheduler {

    const val CHANNEL_ID = "fokus_admission_channel"
    const val CHANNEL_NAME = "Admission Deadlines & Exams"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts for university application deadlines, payment dates, and admission test countdowns"
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFFB7FF00.toInt() // Electric lime
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(context: Context, reminder: AdmissionReminderEntity) {
        if (!reminder.isEnabled || reminder.scheduledTimeMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AdmissionAlarmReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("event_id", reminder.eventId)
            putExtra("title", reminder.title)
            putExtra("message", reminder.message)
        }

        val requestCode = reminder.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.scheduledTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    reminder.scheduledTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Android 12+ exact alarm permission fallback
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminder.scheduledTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AdmissionAlarmReceiver::class.java)
        val requestCode = reminderId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

class AdmissionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "FOKUS Admission Alert"
        val message = intent.getStringExtra("message") ?: "Upcoming university deadline or test."
        val eventId = intent.getStringExtra("event_id")

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "admission")
            putExtra("target_event_id", eventId)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            (eventId?.hashCode() ?: System.currentTimeMillis().toInt()),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        AdmissionNotificationScheduler.createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, AdmissionNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(0xFFB7FF00.toInt())
            .setContentIntent(contentPendingIntent)
            .build()

        notificationManager?.notify((eventId?.hashCode() ?: 7001), notification)
    }
}

class AdmissionBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            CoroutineScope(Dispatchers.IO).launch {
                val db = FokusDatabase.getInstance(context)
                val activeReminders = db.fokusDao().getActiveFutureRemindersSync(System.currentTimeMillis())
                activeReminders.forEach { reminder ->
                    AdmissionNotificationScheduler.scheduleReminder(context, reminder)
                }
            }
        }
    }
}
