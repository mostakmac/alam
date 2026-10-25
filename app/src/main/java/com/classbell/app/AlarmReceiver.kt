package com.classbell.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: return
        val weekday = intent.getIntExtra(EXTRA_WEEKDAY, -1)
        val schedule = AlarmStorage.schedules(context).firstOrNull { it.id == id } ?: return
        AlarmScheduler.scheduleNext(context, schedule, weekday)

        val serviceIntent = Intent(context, AlarmRingingService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (error: IllegalStateException) {
            Log.e("ClassBell", "Android did not allow the class reminder service to start", error)
        } catch (error: SecurityException) {
            Log.e("ClassBell", "Could not start the class reminder alarm", error)
        }
    }

    companion object {
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_WEEKDAY = "weekday"
    }
}
