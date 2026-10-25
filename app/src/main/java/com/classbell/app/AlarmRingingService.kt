package com.classbell.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.io.IOException

class AlarmRingingService : Service() {
    private var player: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            return START_NOT_STICKY
        }
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startRingtone()
        return START_STICKY
    }

    private fun startRingtone() {
        stopPlayer()
        val selected = AlarmStorage.ringtone(this)?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                isLooping = true
                setDataSource(this@AlarmRingingService, selected)
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, what, extra ->
                    Log.e("ClassBell", "Ringtone playback failed: $what/$extra")
                    stopPlayer()
                    if (selected != RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)) {
                        startDefaultRingtone()
                    }
                    true
                }
                prepareAsync()
            }
        } catch (error: IOException) {
            Log.e("ClassBell", "Could not read the selected ringtone", error)
            startDefaultRingtone()
        } catch (error: IllegalArgumentException) {
            Log.e("ClassBell", "Selected ringtone URI is invalid", error)
            startDefaultRingtone()
        } catch (error: SecurityException) {
            Log.e("ClassBell", "Selected ringtone permission is unavailable", error)
            startDefaultRingtone()
        } catch (error: IllegalStateException) {
            Log.e("ClassBell", "Could not load selected ringtone", error)
            startDefaultRingtone()
        }
    }

    private fun startDefaultRingtone() {
        val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: return
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                )
                isLooping = true
                setDataSource(this@AlarmRingingService, fallback)
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        } catch (error: IOException) {
            Log.e("ClassBell", "Could not read the system alarm ringtone", error)
        } catch (error: IllegalArgumentException) {
            Log.e("ClassBell", "System alarm ringtone URI is invalid", error)
        } catch (error: SecurityException) {
            Log.e("ClassBell", "System alarm ringtone permission is unavailable", error)
        } catch (error: IllegalStateException) {
            Log.e("ClassBell", "Could not play the system alarm ringtone", error)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            2,
            Intent(this, RingingActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, AlarmRingingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Class reminder")
            .setContentText("Your class starts in 20 minutes.")
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_MAX)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .setFullScreenIntent(openIntent, true)
            .addAction(android.R.drawable.ic_media_pause, "Dismiss", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Class reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for upcoming class times"
            setSound(null, null)
            enableVibration(true)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun stopPlayer() {
        player?.let {
            it.release()
        }
        player = null
    }

    private fun stopAlarm() {
        stopPlayer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopPlayer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.classbell.app.STOP_ALARM"
        private const val CHANNEL_ID = "class_reminders"
        private const val NOTIFICATION_ID = 1001
    }
}
