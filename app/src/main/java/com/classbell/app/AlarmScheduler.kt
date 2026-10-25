package com.classbell.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.json.JSONArray
import java.util.Calendar

object AlarmScheduler {
    private const val PREFS = "classbell_alarm_ids"
    private const val LEAD_TIME_MILLIS = 20 * 60 * 1000L

    fun canScheduleExact(context: Context): Boolean {
        val manager = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
    }

    fun scheduleAll(context: Context): Boolean {
        if (!canScheduleExact(context)) return false
        val manager = context.getSystemService(AlarmManager::class.java)
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = JSONArray(preferences.getString("keys", "[]") ?: "[]")
        for (index in 0 until previous.length()) {
            val key = previous.optString(index)
            val split = key.lastIndexOf(':')
            if (split > 0) {
                cancel(context, key.substring(0, split), key.substring(split + 1).toIntOrNull() ?: continue)
            }
        }

        val keys = JSONArray()
        AlarmStorage.schedules(context).forEach { schedule ->
            schedule.weekdays.forEach { weekday ->
                val key = "${schedule.id}:$weekday"
                val intent = alarmIntent(context, schedule.id, weekday)
                val pending = PendingIntent.getBroadcast(
                    context,
                    requestCode(schedule.id, weekday),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val triggerAt = nextReminderTime(schedule, weekday)
                try {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                    keys.put(key)
                } catch (error: SecurityException) {
                    Log.e("ClassBell", "Exact alarm permission was revoked while scheduling", error)
                    return false
                }
            }
        }
        preferences.edit().putString("keys", keys.toString()).apply()
        return true
    }

    fun scheduleNext(context: Context, schedule: ClassSchedule, weekday: Int) {
        if (!canScheduleExact(context) || weekday !in schedule.weekdays) return
        val manager = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(schedule.id, weekday),
            alarmIntent(context, schedule.id, weekday),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextReminderTime(schedule, weekday),
                pending
            )
        } catch (error: SecurityException) {
            Log.e("ClassBell", "Could not schedule the next class reminder", error)
        }
    }

    private fun nextReminderTime(schedule: ClassSchedule, weekday: Int): Long {
        val now = Calendar.getInstance()
        val classTime = now.clone() as Calendar
        val daysUntil = (weekday - now.get(Calendar.DAY_OF_WEEK) + 7) % 7
        classTime.add(Calendar.DAY_OF_YEAR, daysUntil)
        classTime.set(Calendar.HOUR_OF_DAY, schedule.hour)
        classTime.set(Calendar.MINUTE, schedule.minute)
        classTime.set(Calendar.SECOND, 0)
        classTime.set(Calendar.MILLISECOND, 0)
        if (classTime.timeInMillis - LEAD_TIME_MILLIS <= now.timeInMillis) {
            classTime.add(Calendar.DAY_OF_YEAR, 7)
        }
        return classTime.timeInMillis - LEAD_TIME_MILLIS
    }

    private fun cancel(context: Context, id: String, weekday: Int) {
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(id, weekday),
            alarmIntent(context, id, weekday),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pending)
        pending.cancel()
    }

    private fun alarmIntent(context: Context, id: String, weekday: Int) =
        Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_SCHEDULE_ID, id)
            .putExtra(AlarmReceiver.EXTRA_WEEKDAY, weekday)

    private fun requestCode(id: String, weekday: Int): Int = 31 * id.hashCode() + weekday
}
