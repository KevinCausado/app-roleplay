package com.elara.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.elara.app.data.local.AppDatabase
import com.elara.app.ui.main.StreakManager

class DailyReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val streak = StreakManager.getStreak(context)
        val todayKey = StreakManager.todayKeyPublic(context)
        val lastDay = context.getSharedPreferences("elara_streak", Context.MODE_PRIVATE)
            .getString("last_day", "")

        if (lastDay != todayKey) {
            NotificationHelper.showDailyReminder(context)
        }

        val db = AppDatabase.get(context)
        val dueWords = db.wordDao().getDueForReview()
        if (dueWords.isNotEmpty()) {
            NotificationHelper.showReviewReminder(context, dueWords.size)
        }

        return Result.success()
    }
}
