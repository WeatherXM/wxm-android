package com.weatherxm.service

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
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
import com.weatherxm.R
import com.weatherxm.data.models.RemoteMessageType
import com.weatherxm.data.models.WXMRemoteMessage
import com.weatherxm.service.workers.RefreshFcmApiWorker
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_ID
import com.weatherxm.ui.common.Contracts.ARG_REMOTE_MESSAGE
import com.weatherxm.ui.common.Contracts.ARG_TYPE
import com.weatherxm.ui.common.Contracts.ARG_URL
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.deeplinkrouter.DeepLinkRouterActivity
import com.weatherxm.util.AndroidBuildInfo
import com.weatherxm.util.hasPermission
import timber.log.Timber
import kotlin.random.Random

class MessagingService : FirebaseMessagingService() {

    /**
     * Suppress InlinedApi as we check for API level before using it through AndroidBuildInfo.sdkInt
     */
    @SuppressLint("InlinedApi")
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("From: ${remoteMessage.from}")

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Timber.d("Message Data payload: ${remoteMessage.data}")
            Timber.d("Message Notification Body: ${it.body}")

            val showNotification = if (AndroidBuildInfo.sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                hasPermission(POST_NOTIFICATIONS)
            } else {
                NotificationManagerCompat.from(this).areNotificationsEnabled()
            }

            if (showNotification) {
                handleNotification(remoteMessage, this)
            }
        }
    }

    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Timber.d("Refreshed FCM token: $token")
        /**
         * Init and invoke the work manager to update FCM token in the server
         */
        RefreshFcmApiWorker.initAndRefreshToken(applicationContext, token)
    }

    /**
     * Suppress NewApi as we check for API level before using it through AndroidBuildInfo.sdkInt
     */
    @SuppressLint("NewApi")
    private fun handleNotification(remoteMessage: RemoteMessage, context: Context) {
        val type =
            RemoteMessageType.parse(remoteMessage.data.getOrDefault(ARG_TYPE, String.empty()))

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (AndroidBuildInfo.sdkInt >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                type.id, type.publicName, NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = type.desc
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, type.id).apply {
            createPendingIntent(context, remoteMessage, type)?.let {
                setContentIntent(it)
            }
            setSmallIcon(R.drawable.ic_logo)
            setContentTitle(remoteMessage.notification?.title)
            setContentText(remoteMessage.notification?.body)
            setAutoCancel(true)
        }.build()

        /**
         * As long as the title of the notification and the ID are different,
         * different notifications will show up:
         * https://developer.android.com/reference/android/app/NotificationManager#notify(java.lang.String,%20int,%20android.app.Notification)
         *
         * We use this functionality in order to replace any notifications that might be bugged.
         * By sending a notification from Firebase will the same title as the previous one,
         * that will replace the shown notification's info with the updated one.
         */
        manager.notify(remoteMessage.notification?.title, 0, notification)
    }

    private fun createPendingIntent(
        context: Context,
        remoteMessage: RemoteMessage,
        type: RemoteMessageType
    ): PendingIntent? {
        val intent = Intent(context, DeepLinkRouterActivity::class.java)
        val wxmRemoteMessage = when (type) {
            RemoteMessageType.STATION -> {
                WXMRemoteMessage(
                    type, deviceId = remoteMessage.data.getOrDefault(ARG_DEVICE_ID, String.empty())
                )
            }
            RemoteMessageType.ANNOUNCEMENT -> {
                WXMRemoteMessage(
                    type, url = remoteMessage.data.getOrDefault(ARG_URL, String.empty())
                )
            }
            else -> {
                return null
            }
        }
        intent.putExtra(ARG_REMOTE_MESSAGE, wxmRemoteMessage)

        /**
         * In order to have multiple distinct intents we need to use unique requestCodes:
         * https://developer.android.com/reference/android/app/PendingIntent.html
         * So we generate a random number from 0-100 which is simple enough to sufficiently
         * achieve it
         */
        val requestCode = Random.nextInt()
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
