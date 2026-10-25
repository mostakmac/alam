package com.classbell.app

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {
    private var selectedHour = 9
    private var selectedMinute = 0
    private val selectedDays = mutableSetOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY
    )
    private lateinit var timeButton: Button
    private lateinit var scheduleList: LinearLayout
    private lateinit var wallpaperImage: ImageView
    private lateinit var wallpaperOverlay: View
    private lateinit var ringtoneButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        buildScreen()
        refreshSchedules()
        refreshRingtoneLabel()
        refreshWallpaper()
    }

    override fun onResume() {
        super.onResume()
        if (::scheduleList.isInitialized) {
            refreshSchedules()
            AlarmScheduler.scheduleAll(this)
        }
    }

    private fun buildScreen() {
        val frame = FrameLayout(this)
        wallpaperImage = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        wallpaperOverlay = View(this).apply {
            setBackgroundColor(Color.argb(218, 18, 20, 42))
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(36))
        }
        scroll.addView(content)
        frame.addView(wallpaperImage)
        frame.addView(wallpaperOverlay)
        frame.addView(scroll)
        setContentView(frame)

        content.addView(label("CLASSBELL", 14f, Color.rgb(192, 182, 255), true))
        content.addView(label("Your day, right on time.", 30f, Color.WHITE, true).apply {
            setPadding(0, dp(8), 0, dp(8))
        })
        content.addView(label("Set a class time and we’ll ring 20 minutes before it starts.", 16f, Color.LTGRAY))

        val scheduleCard = card()
        scheduleCard.addView(label("Add a class", 21f, Color.WHITE, true))
        timeButton = button("Class time  ·  9:00 AM") {
            TimePickerDialog(this, { _, hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                timeButton.text = "Class time  ·  ${formatTime(hour, minute)}"
            }, selectedHour, selectedMinute, false).show()
        }
        scheduleCard.addView(timeButton, matchWrap())
        scheduleCard.addView(label("Repeat on", 15f, Color.LTGRAY).apply {
            setPadding(0, dp(18), 0, dp(6))
        })
        val days = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val dayLabels = listOf(
            Calendar.MONDAY to "M",
            Calendar.TUESDAY to "T",
            Calendar.WEDNESDAY to "W",
            Calendar.THURSDAY to "T",
            Calendar.FRIDAY to "F",
            Calendar.SATURDAY to "S",
            Calendar.SUNDAY to "S"
        )
        dayLabels.forEach { (day, text) ->
            val check = CheckBox(this).apply {
                this.text = text
                textSize = 12f
                buttonTintList = android.content.res.ColorStateList.valueOf(Color.rgb(192, 182, 255))
                isChecked = day in selectedDays
                setTextColor(Color.WHITE)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedDays.add(day) else selectedDays.remove(day)
                }
            }
            days.addView(check, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        scheduleCard.addView(days)
        scheduleCard.addView(button("Save class reminder") { addSchedule() }, matchWrap())
        content.addView(scheduleCard, matchWrap(dp(20)))

        content.addView(label("Your class reminders", 22f, Color.WHITE, true).apply {
            setPadding(0, dp(28), 0, dp(10))
        })
        scheduleList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(scheduleList, matchWrap())

        val settingsCard = card()
        settingsCard.addView(label("Make it yours", 21f, Color.WHITE, true))
        ringtoneButton = button("Choose alarm ringtone") { chooseRingtone() }
        settingsCard.addView(ringtoneButton, matchWrap())
        settingsCard.addView(button("Change alarm wallpaper") { chooseWallpaper() }, matchWrap())
        settingsCard.addView(label("Shown behind your alarm and on this screen. Your class reminders stay saved.", 13f, Color.LTGRAY).apply {
            setPadding(0, dp(8), 0, 0)
        })
        content.addView(settingsCard, matchWrap(dp(20)))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !AlarmScheduler.canScheduleExact(this)) {
            content.addView(label("Allow exact alarms in Android settings so reminders ring at the right time.", 14f, Color.rgb(255, 220, 145)).apply {
                setPadding(0, dp(18), 0, 0)
            })
        }
    }

    private fun addSchedule() {
        if (selectedDays.isEmpty()) {
            toast("Choose at least one day for your class.")
            return
        }
        val schedules = AlarmStorage.schedules(this).toMutableList()
        schedules.add(ClassSchedule(hour = selectedHour, minute = selectedMinute, weekdays = selectedDays.toSet()))
        AlarmStorage.saveSchedules(this, schedules)
        refreshSchedules()
        if (!AlarmScheduler.scheduleAll(this)) {
            requestExactAlarmAccess()
        } else {
            toast("Class reminder saved. It will ring 20 minutes before class.")
        }
    }

    private fun removeSchedule(schedule: ClassSchedule) {
        AlarmStorage.saveSchedules(this, AlarmStorage.schedules(this).filterNot { it.id == schedule.id })
        if (!AlarmScheduler.scheduleAll(this)) {
            toast("Reminder removed, but Android exact-alarm access is unavailable.")
        }
        refreshSchedules()
    }

    private fun refreshSchedules() {
        if (!::scheduleList.isInitialized) return
        scheduleList.removeAllViews()
        val schedules = AlarmStorage.schedules(this)
        if (schedules.isEmpty()) {
            scheduleList.addView(label("No class reminders yet.", 15f, Color.LTGRAY).apply {
                setPadding(0, dp(4), 0, dp(12))
            })
            return
        }
        schedules.forEach { schedule ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(10), dp(14))
                background = rounded(Color.argb(210, 36, 38, 67), dp(18))
            }
            val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            details.addView(label(formatTime(schedule.hour, schedule.minute), 20f, Color.WHITE, true))
            val dayNames = schedule.weekdays.sorted().joinToString(" · ") { weekdayName(it) }
            details.addView(label("$dayNames  ·  rings 20 min early", 13f, Color.LTGRAY).apply {
                setPadding(0, dp(4), 0, 0)
            })
            row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
            val remove = button("Remove") { removeSchedule(schedule) }
            row.addView(remove, wrapWrap())
            scheduleList.addView(row, matchWrap(dp(9)))
        }
    }

    private fun chooseRingtone() {
        val current = AlarmStorage.ringtone(this)?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose class alarm ringtone")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)
        }
        startActivityForResult(intent, REQUEST_RINGTONE)
    }

    private fun chooseWallpaper() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_WALLPAPER)
    }

    @Deprecated("The platform pickers return results through the activity result contract.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        when (requestCode) {
            REQUEST_RINGTONE -> {
                val uri = data.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                AlarmStorage.saveRingtone(this, uri?.toString())
                refreshRingtoneLabel()
                toast("Alarm ringtone updated.")
            }
            REQUEST_WALLPAPER -> {
                val uri = data.data ?: return
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    AlarmStorage.saveWallpaper(this, uri.toString())
                    refreshWallpaper()
                    toast("Alarm wallpaper updated.")
                } catch (error: SecurityException) {
                    toast("Could not save access to that image. Please choose another.")
                }
            }
        }
    }

    private fun refreshRingtoneLabel() {
        if (!::ringtoneButton.isInitialized) return
        val uri = AlarmStorage.ringtone(this)?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val name = uri?.let { RingtoneManager.getRingtone(this, it)?.getTitle(this) } ?: "System default"
        ringtoneButton.text = "Ringtone  ·  $name"
    }

    private fun refreshWallpaper() {
        if (!::wallpaperImage.isInitialized) return
        val uriString = AlarmStorage.wallpaper(this)
        if (uriString == null) {
            wallpaperImage.setImageDrawable(null)
            wallpaperOverlay.setBackgroundColor(Color.argb(218, 18, 20, 42))
            return
        }
        try {
            wallpaperImage.setImageURI(Uri.parse(uriString))
            wallpaperOverlay.setBackgroundColor(Color.argb(190, 18, 20, 42))
        } catch (error: SecurityException) {
            AlarmStorage.saveWallpaper(this, null)
            wallpaperImage.setImageDrawable(null)
            toast("Saved wallpaper is no longer available. Choose a new one.")
        }
    }

    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            toast("Allow exact alarms to activate your class reminders on time.")
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:$packageName"))
            )
        } else {
            toast("Could not schedule the class reminder. Please try again.")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(Color.argb(220, 36, 38, 67), dp(22))
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        textSize = 15f
        setOnClickListener { action() }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun matchWrap(bottom: Int = 0) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            if (bottom > 0) bottomMargin = bottom
        }

    private fun wrapWrap() =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(calendar.time)
    }

    private fun weekdayName(day: Int): String = when (day) {
        Calendar.MONDAY -> "Mon"
        Calendar.TUESDAY -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY -> "Thu"
        Calendar.FRIDAY -> "Fri"
        Calendar.SATURDAY -> "Sat"
        else -> "Sun"
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    companion object {
        private const val REQUEST_RINGTONE = 1
        private const val REQUEST_WALLPAPER = 2
        private const val REQUEST_NOTIFICATIONS = 3
    }
}
