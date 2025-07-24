package com.weatherxm.ui.devicenotifications

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityDeviceNotificationsBinding
import com.weatherxm.service.workers.DevicesNotificationsWorker
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.compose.SwitchWithIcon
import com.weatherxm.util.AndroidBuildInfo
import com.weatherxm.util.checkPermissionsAndThen
import com.weatherxm.util.hasPermission
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class DeviceNotificationsActivity : BaseActivity() {
    private val model: DeviceNotificationsViewModel by viewModel {
        parametersOf(intent.parcelable<UIDevice>(ARG_DEVICE))
    }
    private lateinit var binding: ActivityDeviceNotificationsBinding
    private var hasNotificationPermissions = false

    /**
     * Suppress InlinedApi as we check for API level before using it through AndroidBuildInfo.sdkInt
     */
    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (model.device.isEmpty()) {
            Timber.d("Could not start DeviceNotificationsActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.deviceName.text = model.device.getDefaultOrFriendlyName()
        handleOwnershipIcon()

        hasNotificationPermissions = if (AndroidBuildInfo.sdkInt >= TIRAMISU) {
            hasPermission(POST_NOTIFICATIONS)
        } else {
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        }

        handleMainSwitch()
        handleNotificationCategories()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.STATION_NOTIFICATIONS, classSimpleName())
    }

    /**
     * Suppress InlinedApi as we check for API level before using it through AndroidBuildInfo.sdkInt
     */
    @SuppressLint("InlinedApi")
    private fun handleMainSwitch() {
        model.getDeviceNotificationsEnabled(hasNotificationPermissions)

        binding.mainSwitch.setContent {
            SwitchWithIcon(isChecked = model.notificationsEnabled.value) {
                if (it) {
                    if (hasNotificationPermissions) {
                        onNotificationsEnabled()
                    } else if (AndroidBuildInfo.sdkInt >= TIRAMISU) {
                        checkPermissionsAndThen(
                            permissions = arrayOf(POST_NOTIFICATIONS),
                            onGranted = { onNotificationsEnabled() },
                            onDenied = { model.setDeviceNotificationsEnabled(false) }
                        )
                    }
                } else {
                    analytics.trackEventUserAction(
                        AnalyticsService.ParamValue.TOGGLE_STATION_NOTIFICATIONS.paramValue,
                        null,
                        Pair(
                            AnalyticsService.CustomParam.ACTION.paramName,
                            AnalyticsService.ParamValue.DISABLE.paramValue
                        )
                    )
                    model.setDeviceNotificationsEnabled(false)
                }
            }
        }
    }

    private fun onNotificationsEnabled() {
        analytics.trackEventUserAction(
            actionName = AnalyticsService.ParamValue.TOGGLE_STATION_NOTIFICATIONS.paramValue,
            contentType = null,
            Pair(
                AnalyticsService.CustomParam.ACTION.paramName,
                AnalyticsService.ParamValue.ENABLE.paramValue
            )
        )
        DevicesNotificationsWorker.initAndStart(this)
        model.setDeviceNotificationsEnabled(true)
    }

    private fun handleNotificationCategories() {
        model.getDeviceNotificationTypes()

        binding.notificationTypes.setContent {
            DeviceNotificationTypesView(
                isMainEnabled = model.notificationsEnabled.value,
                supportsFirmwareUpdate = model.device.isHelium(),
                notificationTypesEnabled = model.notificationTypesEnabled
            ) { type, enabled ->
                val actionParam = if (enabled) {
                    AnalyticsService.ParamValue.ENABLE.paramValue
                } else {
                    AnalyticsService.ParamValue.DISABLE.paramValue
                }
                analytics.trackEventUserAction(
                    AnalyticsService.ParamValue.TOGGLE_STATION_NOTIFICATION_TYPE.paramValue,
                    null,
                    Pair(
                        AnalyticsService.CustomParam.ACTION.paramName,
                        actionParam
                    ),
                    Pair(
                        FirebaseAnalytics.Param.SOURCE,
                        type.analyticsParam.paramValue
                    )
                )
                model.setDeviceNotificationTypeEnabled(type, enabled)
            }
        }
    }

    private fun handleOwnershipIcon() {
        with(binding.ownershipIcon) {
            when (model.device.relation) {
                DeviceRelation.OWNED -> {
                    setImageResource(R.drawable.ic_home)
                    setColor(R.color.colorOnSurface)
                }
                DeviceRelation.FOLLOWED -> {
                    setImageResource(R.drawable.ic_favorite)
                    setColor(R.color.follow_heart_color)
                }
                else -> visible(false)
            }
        }
    }
}
