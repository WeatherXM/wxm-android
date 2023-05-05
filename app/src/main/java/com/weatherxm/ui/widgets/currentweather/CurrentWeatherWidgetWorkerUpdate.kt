package com.weatherxm.ui.widgets.currentweather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.weatherxm.R
import com.weatherxm.ui.common.Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE
import com.weatherxm.util.WidgetHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CurrentWeatherWidgetWorkerUpdate(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {
    companion object {
        const val UPDATE_INTERVAL_IN_MINS = 15L
    }

    private val widgetHelper: WidgetHelper by inject()

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as
        NotificationManager

    override suspend fun doWork(): Result {
        return widgetHelper.getWidgetIds().fold({
            Result.failure()
        }) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            val ids = it.map { id ->
                id.toInt()
            }
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids.toIntArray())
            intent.putExtra(ARG_IS_CUSTOM_APPWIDGET_UPDATE, true)
            context.sendBroadcast(intent)

            setForeground(createForegroundInfo())

            Result.success()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val id = applicationContext.getString(R.string.updating_weather_notification_id)
        val title = applicationContext.getString(R.string.updating_weather_notification_title)

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val mChannel = NotificationChannel(id, title, NotificationManager.IMPORTANCE_HIGH)
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            notificationManager.createNotificationChannel(mChannel)
        }

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setPriority(PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(false)
            .build()

        return ForegroundInfo(0, notification)
    }

}
