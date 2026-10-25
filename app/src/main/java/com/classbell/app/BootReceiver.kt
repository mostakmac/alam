package com.classbell.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            if (!AlarmScheduler.scheduleAll(context)) {
                Log.w("ClassBell", "Class reminders were not restored because exact-alarm access is unavailable")
            }
        }
    }
}
