package edu.uw.ischool.gehuijun.quizdroid

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.preference.PreferenceManager

class PeriodicDownloadScheduler(private val context: Context) {

    fun schedulePeriodicDownload(url: String) {
        // Get the user-defined interval for periodic downloads from preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val downloadIntervalStr = sharedPreferences.getString("download_interval", "")
        val downloadInterval = downloadIntervalStr?.toLongOrNull() ?: 60

        // Schedule the download service using AlarmManager
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createDownloadServicePendingIntent(url)

        // Set the repeating alarm
        val intervalMillis = downloadInterval * 60 * 1000
        val initialDelayMillis = SystemClock.elapsedRealtime() + intervalMillis
        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            initialDelayMillis,
            intervalMillis,
            pendingIntent
        )
    }

    private fun createDownloadServicePendingIntent(url: String): PendingIntent {
        val intent = Intent(context, DownloadService::class.java).apply {
            putExtra(DownloadService.EXTRA_URL, url)
        }
        return PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
