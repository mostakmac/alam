package com.classbell.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.net.Uri
import android.util.Log

class RingingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.decorView.setBackgroundColor(Color.rgb(21, 21, 43))

        val frame = FrameLayout(this)
        val wallpaper = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        AlarmStorage.wallpaper(this)?.let { uri ->
            try {
                wallpaper.setImageURI(Uri.parse(uri))
            } catch (error: SecurityException) {
                Log.e("ClassBell", "Saved alarm wallpaper is no longer accessible", error)
            }
        }
        val overlay = android.view.View(this).apply {
            background = GradientDrawable().apply { setColor(Color.argb(205, 21, 21, 43)) }
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        val title = TextView(this).apply {
            text = "Time to get ready"
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "Your class starts in 20 minutes"
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 40)
        }
        val dismiss = Button(this).apply {
            text = "Dismiss alarm"
            setOnClickListener {
                stopService(Intent(this@RingingActivity, AlarmRingingService::class.java).setAction(AlarmRingingService.ACTION_STOP))
                finish()
            }
        }
        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(dismiss)
        frame.addView(wallpaper)
        frame.addView(overlay)
        frame.addView(layout)
        setContentView(frame)
    }
}
