package com.classbell.app

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

data class ClassSchedule(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val weekdays: Set<Int>
)

object AlarmStorage {
    private const val PREFS = "classbell_settings"
    private const val SCHEDULES = "schedules"
    private const val RINGTONE = "ringtone_uri"
    private const val WALLPAPER = "wallpaper_uri"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun schedules(context: Context): List<ClassSchedule> {
        val data = prefs(context).getString(SCHEDULES, "[]") ?: "[]"
        return try {
            val array = JSONArray(data)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val days = item.optJSONArray("weekdays") ?: JSONArray()
                val weekdays = (0 until days.length()).mapNotNull { days.optInt(it, -1).takeIf { day -> day in 1..7 } }.toSet()
                val hour = item.optInt("hour", -1)
                val minute = item.optInt("minute", -1)
                if (hour !in 0..23 || minute !in 0..59 || weekdays.isEmpty()) {
                    null
                } else {
                    ClassSchedule(item.optString("id"), hour, minute, weekdays)
                }
            }
        } catch (error: JSONException) {
            Log.e("ClassBell", "Could not read saved class schedules", error)
            emptyList()
        }
    }

    fun saveSchedules(context: Context, schedules: List<ClassSchedule>) {
        val array = JSONArray()
        schedules.forEach { schedule ->
            array.put(
                JSONObject()
                    .put("id", schedule.id)
                    .put("hour", schedule.hour)
                    .put("minute", schedule.minute)
                    .put("weekdays", JSONArray(schedule.weekdays.sorted()))
            )
        }
        prefs(context).edit().putString(SCHEDULES, array.toString()).apply()
    }

    fun ringtone(context: Context): String? = prefs(context).getString(RINGTONE, null)

    fun saveRingtone(context: Context, uri: String?) {
        prefs(context).edit().putString(RINGTONE, uri).apply()
    }

    fun wallpaper(context: Context): String? = prefs(context).getString(WALLPAPER, null)

    fun saveWallpaper(context: Context, uri: String?) {
        prefs(context).edit().putString(WALLPAPER, uri).apply()
    }
}
