package com.elara.app.ui.main

import android.content.Context
import java.util.Calendar

object StreakManager {

    private const val PREFS = "elara_streak"

    fun recordToday(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = todayKey()
        val lastDay = prefs.getString("last_day", "") ?: ""
        val streak = prefs.getInt("streak", 0)

        if (lastDay == today) return

        val yesterday = yesterdayKey()
        val newStreak = if (lastDay == yesterday) streak + 1 else 1

        prefs.edit()
            .putString("last_day", today)
            .putInt("streak", newStreak)
            .apply()
    }

    fun getStreak(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastDay = prefs.getString("last_day", "") ?: ""
        val streak = prefs.getInt("streak", 0)
        return if (lastDay == todayKey() || lastDay == yesterdayKey()) streak else 0
    }

    private fun todayKey(): String {
        val c = Calendar.getInstance()
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
    }

    private fun yesterdayKey(): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -1)
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
    }
}
