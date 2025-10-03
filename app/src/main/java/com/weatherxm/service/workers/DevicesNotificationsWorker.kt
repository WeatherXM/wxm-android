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
import com.weatherxm.util.isToday
import com.weatherxm.util.isYesterday
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class DevicesNotificationsWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {
    companion object {
        private val UPDATE_INTERVAL = 2.hours.toJavaDuration()
        private const val WORK_NAME = "DEVICES_NOTIFICATIONS_WORKER"
        private const val QOD_THRESHOLD = 80

        fun stopWorker(context: Context) {
            Timber.d("[Devices BG Worker]: Stopping Work Manager for devices workers.")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun initAndStart(context: Context) {
            Timber.d("[Devices BG Worker]: Starting Work Manager for devices.")

            val request = PeriodicWorkRequestBuilder<DevicesNotificationsWorker>(UPDATE_INTERVAL)
                .setConstraints(Constraints.Companion.requireNetwork())
                .setInitialDelay(UPDATE_INTERVAL)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
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
            stopWorker(context)
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
                    stopWorker(context)
                    Result.success()
                }
                is ApiError.GenericError.JWTError.ForbiddenError -> {
                    Timber.w("[Devices BG Worker]: JWTError.ForbiddenError.")
                    stopWorker(context)
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
            stopWorker(context)
            return
        }

        Timber.d("[Devices BG Worker]: Devices with notifications enabled found.")
        ownedDevicesWithNotifications.forEach {
            val typesEnabled = notificationsRepo.getDeviceNotificationTypesEnabled(it.id)

            if (!it.isOnline() && typesEnabled.contains(DeviceNotificationType.ACTIVITY)) {
                Timber.d("[Devices BG Worker]: Is offline ${it.id}")
                sendNotification(
                    titleResId = R.string.station_inactive,
                    bodyResId = R.string.station_inactive_notification_msg,
                    device = it,
                    notificationType = DeviceNotificationType.ACTIVITY
                )
            }
            if (typesEnabled.contains(DeviceNotificationType.BATTERY)) {
                Timber.d("[Devices BG Worker]: Has low battery ${it.id}")
                if (it.hasLowBattery == true) {
                    sendNotification(
                        titleResId = R.string.station_low_battery,
                        bodyResId = R.string.station_low_battery_notification_msg,
                        device = it,
                        notificationType = DeviceNotificationType.BATTERY
                    )
                } else if (it.hasLowGwBattery == true) {
                    sendNotification(
                        titleResId = R.string.low_gw_battery,
                        bodyResId = R.string.station_low_battery_notification_msg,
                        device = it,
                        notificationType = DeviceNotificationType.BATTERY
                    )
                }
            }
            if (it.shouldPromptUpdate() && typesEnabled.contains(DeviceNotificationType.FIRMWARE)) {
                Timber.d("[Devices BG Worker]: Has firmware update ${it.id}")
                sendNotification(
                    titleResId = R.string.firmware_update,
                    bodyResId = R.string.firmware_update_notification_msg,
                    device = it,
                    notificationType = DeviceNotificationType.FIRMWARE
                )
            }
            if (it.notifyOfBadHealth() && typesEnabled.contains(DeviceNotificationType.HEALTH)) {
                Timber.d("[Devices BG Worker]: Has health issues ${it.id}")
                sendNotification(
                    titleResId = R.string.station_health_issues,
                    bodyResId = R.string.station_health_issues_notification_msg,
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

    /**
     * In all types except Activity -> If we haven't sent already today a notification
     *
     * In Activity -> If `lastWeatherStationActivity` is after the last notification sent
     * or a day has passed
     */
    private fun shouldSendNotification(device: UIDevice, type: DeviceNotificationType): Boolean {
        val lastSentTimestamp =
            notificationsRepo.getDeviceNotificationTypeTimestamp(device.id, type)

        if (lastSentTimestamp == 0L) {
            return true
        }
        val lastSentDate = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(lastSentTimestamp),
            ZoneId.systemDefault()
        )
        val lastStationActivity = device.lastWeatherStationActivity

        return if (type == DeviceNotificationType.ACTIVITY && lastStationActivity != null) {
            lastStationActivity.isAfter(lastSentDate) || !lastSentDate.isToday()
        } else {
            !lastSentDate.isToday()
        }
    }

    private fun sendNotification(
        titleResId: Int,
        bodyResId: Int,
        device: UIDevice,
        notificationType: DeviceNotificationType
    ) {
        /**
         * Requirements as specified in `shouldSendNotification` not met, returning.
         */
        if (!shouldSendNotification(device, notificationType)) {
            Timber.d("[Devices BG Worker]: Ignoring -> ${notificationType.name}: ${device.id}")
            return
        }

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
            setContentTitle(context.getString(titleResId))
            setContentText(context.getString(bodyResId, device.getDefaultOrFriendlyName()))
            setAutoCancel(true)
        }.build()

        /**
         * As long as the IDs are different a different notification will show up:
         * https://developer.android.com/reference/android/app/NotificationManager#notify(java.lang.String,%20int,%20android.app.Notification)
         */
        val notificationId = notificationType.name.hashCode() + device.id.hashCode()
        manager.notify(null, notificationId, notification)

        notificationsRepo.setDeviceNotificationTypeTimestamp(device.id, notificationType)
    }

    private fun createPendingIntent(
        context: Context,
        device: UIDevice,
        notificationType: DeviceNotificationType
    ): PendingIntent? {
        val deviceDetailsActivity = Intent(context, DeviceDetailsActivity::class.java)
            .putExtra(Contracts.ARG_DEVICE, device)
            .putExtra(Contracts.ARG_OPEN_STATION_FROM_NOTIFICATION, true)

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
