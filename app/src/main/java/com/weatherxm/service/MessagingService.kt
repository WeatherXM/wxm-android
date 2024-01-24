package com.weatherxm.service

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.RemoteMessage.Notification
import com.weatherxm.R
import com.weatherxm.ui.urlrouteractivity.UrlRouterActivity
import com.weatherxm.util.hasPermission
import timber.log.Timber

class MessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("Message data payload: ${remoteMessage.data}")
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Timber.d("Message Notification Body: ${it.body}")

            val showNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hasPermission(POST_NOTIFICATIONS)
            } else {
                NotificationManagerCompat.from(this).areNotificationsEnabled()
            }

            if (showNotification) {
                handleNotification(it, this)
            }
        }
    }

    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Timber.d("Refreshed Firebase token: $token")
    }

    private fun handleNotification(remoteNotification: Notification, context: Context) {
        // Create an Intent for the activity you want to start.
        // TODO: REQUEST CODE here
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            5,
            Intent(context, UrlRouterActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, "Announcements").apply {
            setContentIntent(pendingIntent)
            setSmallIcon(R.drawable.ic_logo)
            setContentTitle(remoteNotification.title)
            setContentInfo(remoteNotification.body)
        }.build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "WeatherXM", "Announcements", NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
        // TODO: ID here
        manager.notify(12, notification)
    }
}
