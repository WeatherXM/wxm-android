package com.weatherxm.service.workers

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import arrow.core.Either
import arrow.core.getOrElse
import com.weatherxm.R
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.DeviceNotificationType
import com.weatherxm.data.models.RemoteMessageType
import com.weatherxm.data.models.UserActionError
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.DeviceNotificationsRepository
import com.weatherxm.data.requireNetwork
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.devicedetails.DeviceDetailsActivity
import com.weatherxm.usecases.DeviceListUseCase
import com.weatherxm.util.AndroidBuildInfo
import com.weatherxm.util.hasPermission
import com.weatherxm.util.isYesterday
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class DevicesNotificationsWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {
    companion object {
        // STOPSHIP: TODO: change the below to 2 hours, currently testing.
        private val UPDATE_INTERVAL = 20.minutes.toJavaDuration()
        private const val WORK_NAME = "DEVICES_NOTIFICATIONS_WORKER"
        private const val QOD_THRESHOLD = 80

        fun stopWorkers(context: Context) {
            Timber.d("[Devices BG Worker]: Stopping Work Manager for devices workers.")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun initAndStart(context: Context) {
            Timber.d("[Devices BG Worker]: Starting Work Manager for devices.")

            val request = PeriodicWorkRequestBuilder<DevicesNotificationsWorker>(UPDATE_INTERVAL)
                .setConstraints(Constraints.Companion.requireNetwork())
                .setInitialDelay(UPDATE_INTERVAL)
                .build()

            WorkManager.Companion.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    private val devicesUseCase: DeviceListUseCase by inject()
    private val notificationsRepo: DeviceNotificationsRepository by inject()
    private val authRepo: AuthRepository by inject()

    @SuppressLint("InlinedApi")
    override suspend fun doWork(): Result {
        val hasPermission = if (AndroidBuildInfo.sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            context.hasPermission(POST_NOTIFICATIONS)
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        if (!hasPermission) {
            Timber.d("[Devices BG Worker]: No notifications permissions, stopping worker.")
            stopWorkers(context)
            return Result.failure()
        }
        Timber.d("[Devices BG Worker]: Starting work...")

        return authRepo.isLoggedIn().let { isLoggedIn ->
            if (isLoggedIn) {
                devicesUseCase.getUserDevices().map {
                    checkDevices(it)
                    Result.success()
                }
            } else {
                Either.Left(UserActionError.UserNotLoggedInError())
            }
        }.getOrElse { failure ->
            when (failure) {
                is UserActionError.UserNotLoggedInError -> {
                    Timber.w("[Devices BG Worker]: UserNotLoggedInError.")
                    stopWorkers(context)
                    Result.success()
                }
                is ApiError.GenericError.JWTError.ForbiddenError -> {
                    Timber.w("[Devices BG Worker]: JWTError.ForbiddenError.")
                    stopWorkers(context)
                    Result.success()
                }
                else -> {
                    Timber.w(
                        Exception("Fetching user devices failed: ${failure.code}"),
                        failure.toString()
                    )
                    Result.retry()
                }
            }
        }
    }

    private fun checkDevices(devices: List<UIDevice>) {
        val ownedDevicesWithNotifications = devices.filter {
            it.isOwned() && notificationsRepo.getDeviceNotificationsEnabled(it.id)
        }.ifEmpty {
            Timber.d("[Devices BG Worker]: No owned devices with notifications enabled found.")
            stopWorkers(context)
            return
        }

        Timber.d("[Devices BG Worker]: Devices with notifications enabled found.")
        ownedDevicesWithNotifications.forEach {
            val typesEnabled = notificationsRepo.getDeviceNotificationTypesEnabled(it.id)

            if (!it.isOnline() && typesEnabled.contains(DeviceNotificationType.ACTIVITY)) {
                Timber.d("[Devices BG Worker]: Sending notification for offline device ${it.id}")
                // TODO: Send notification ONLY if previous notification was before the latest activity or a day has passed
                sendNotification(
                    context = context,
                    title = context.getString(R.string.station_inactive),
                    body = context.getString(
                        R.string.station_inactive_notification_msg,
                        it.getDefaultOrFriendlyName()
                    ),
                    device = it,
                    notificationType = DeviceNotificationType.ACTIVITY
                )
            }
            if (it.hasLowBattery == true && typesEnabled.contains(DeviceNotificationType.BATTERY)) {
                Timber.d("[Devices BG Worker]: Sending notification for low battery ${it.id}")
                // TODO: Send notification ONLY if we haven't sent today
                sendNotification(
                    context = context,
                    title = context.getString(R.string.station_low_battery),
                    body = context.getString(
                        R.string.station_low_battery_notification_msg,
                        it.getDefaultOrFriendlyName()
                    ),
                    device = it,
                    notificationType = DeviceNotificationType.BATTERY
                )
            }
            if (it.shouldPromptUpdate() && typesEnabled.contains(DeviceNotificationType.FIRMWARE)) {
                Timber.d("[Devices BG Worker]: Sending notification for firmware update ${it.id}")
                // TODO: Send notification ONLY if we haven't sent today
                sendNotification(
                    context = context,
                    title = context.getString(R.string.firmware_update),
                    body = context.getString(
                        R.string.firmware_update_notification_msg,
                        it.getDefaultOrFriendlyName()
                    ),
                    device = it,
                    notificationType = DeviceNotificationType.FIRMWARE
                )
            }
            if (it.notifyOfBadHealth() && typesEnabled.contains(DeviceNotificationType.HEALTH)) {
                Timber.d("[Devices BG Worker]: Sending notification for health issues ${it.id}")
                // TODO: Send notification ONLY if we haven't sent today
                sendNotification(
                    context = context,
                    title = context.getString(R.string.station_health_issues),
                    body = context.getString(
                        R.string.station_health_issues_notification_msg,
                        it.getDefaultOrFriendlyName()
                    ),
                    device = it,
                    notificationType = DeviceNotificationType.HEALTH
                )
            }
        }
    }

    private fun UIDevice.notifyOfBadHealth(): Boolean {
        val qodBelowThreshold = qodScore != null && qodScore < QOD_THRESHOLD
        val hasPolIssue = polReason != null
        val areDataForPreviousDay = metricsTimestamp != null && metricsTimestamp.isYesterday()
        (qodScore != null && qodScore < QOD_THRESHOLD) || polReason != null

        return areDataForPreviousDay && (qodBelowThreshold || hasPolIssue)
    }

    private fun sendNotification(
        context: Context,
        title: String,
        body: String,
        device: UIDevice,
        notificationType: DeviceNotificationType
    ) {
        val type = RemoteMessageType.STATION

        val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            type.id, type.publicName, NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = type.desc
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, type.id).apply {
            createPendingIntent(context, device, notificationType)?.let {
                setContentIntent(it)
            }
            setSmallIcon(R.drawable.ic_logo)
            setContentTitle(title)
            setContentText(body)
            setAutoCancel(true)
        }.build()

        /**
         * As long as the IDs are different a different notification will show up:
         * https://developer.android.com/reference/android/app/NotificationManager#notify(java.lang.String,%20int,%20android.app.Notification)
         */
        val notificationId = notificationType.name.hashCode() + device.id.hashCode()
        manager.notify(null, notificationId, notification)
    }

    private fun createPendingIntent(
        context: Context,
        device: UIDevice,
        notificationType: DeviceNotificationType
    ): PendingIntent? {
        val deviceDetailsActivity = Intent(context, DeviceDetailsActivity::class.java)
            .putExtra(Contracts.ARG_DEVICE, device)

        /**
         * A unique request code per Intent, to allow different notifications for the same device
         * to open the Device Details screen.
         */
        val requestCode = notificationType.name.hashCode() + device.id.hashCode()

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deviceDetailsActivity)
            if (AndroidBuildInfo.sdkInt >= Build.VERSION_CODES.S) {
                getPendingIntent(
                    requestCode,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            } else {
                getPendingIntent(requestCode, PendingIntent.FLAG_CANCEL_CURRENT)
            }
        }
    }
}
