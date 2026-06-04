package com.elara.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.elara.app.R
import com.elara.app.ui.main.MainActivity

object NotificationHelper {

    private const val CHANNEL_DAILY = "elara_daily"
    private const val CHANNEL_REVIEW = "elara_review"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DAILY, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Daily English practice reminder" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REVIEW, "Word review", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Spaced repetition review alerts" }
        )
    }

    fun showDailyReminder(context: Context) {
        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Elara is waiting...")
            .setContentText("You haven't practiced English today. Keep your streak alive!")
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1001, notification)
    }

    fun showReviewReminder(context: Context, wordCount: Int) {
        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REVIEW)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$wordCount words to review")
            .setContentText("Your spaced repetition session is ready.")
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1002, notification)
    }
}
