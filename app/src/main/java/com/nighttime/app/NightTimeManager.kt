package com.nighttime.app

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * Manages user-selected night hours and checks if the current time
 * falls within the active night window.
 *
 * Night hours are persisted via SharedPreferences so they survive
 * app restarts.
 *
 * Supports wrapping around midnight (e.g., 22:00 → 06:00).
 */
class NightTimeManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "nighttime_prefs"
        private const val KEY_START_HOUR = "start_hour"
        private const val KEY_START_MINUTE = "start_minute"
        private const val KEY_END_HOUR = "end_hour"
        private const val KEY_END_MINUTE = "end_minute"
        private const val KEY_ENABLED = "enabled"

        // Defaults: 10 PM to 7 AM
        const val DEFAULT_START_HOUR = 22
        const val DEFAULT_START_MINUTE = 0
        const val DEFAULT_END_HOUR = 7
        const val DEFAULT_END_MINUTE = 0
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var startHour: Int
        get() = prefs.getInt(KEY_START_HOUR, DEFAULT_START_HOUR)
        set(value) = prefs.edit().putInt(KEY_START_HOUR, value).apply()

    var startMinute: Int
        get() = prefs.getInt(KEY_START_MINUTE, DEFAULT_START_MINUTE)
        set(value) = prefs.edit().putInt(KEY_START_MINUTE, value).apply()

    var endHour: Int
        get() = prefs.getInt(KEY_END_HOUR, DEFAULT_END_HOUR)
        set(value) = prefs.edit().putInt(KEY_END_HOUR, value).apply()

    var endMinute: Int
        get() = prefs.getInt(KEY_END_MINUTE, DEFAULT_END_MINUTE)
        set(value) = prefs.edit().putInt(KEY_END_MINUTE, value).apply()

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /**
     * Returns true if the current time is within the user-selected night window
     * AND the feature is enabled.
     */
    fun isNightTimeNow(): Boolean {
        if (!isEnabled) return false

        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes <= endMinutes) {
            // Same-day window (e.g. 01:00 → 05:00)
            nowMinutes in startMinutes until endMinutes
        } else {
            // Crosses midnight (e.g. 22:00 → 07:00)
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
    }

    /** Formatted start time string for display. */
    fun formattedStart(): String = formatTime(startHour, startMinute)

    /** Formatted end time string for display. */
    fun formattedEnd(): String = formatTime(endHour, endMinute)

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d %s", displayHour, minute, amPm)
    }
}
