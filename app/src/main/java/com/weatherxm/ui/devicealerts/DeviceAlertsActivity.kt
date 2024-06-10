package com.weatherxm.ui.devicealerts

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.DeviceProfile
import com.weatherxm.databinding.ActivityDeviceAlertsBinding
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import timber.log.Timber

class DeviceAlertsActivity : BaseActivity(), DeviceAlertListener {
    private lateinit var binding: ActivityDeviceAlertsBinding

    private var device: UIDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        device = intent?.extras?.parcelable(ARG_DEVICE)
        if (device == null) {
            Timber.d("Could not start DeviceAlertsActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        with(binding.toolbar) {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
        device?.getDefaultOrFriendlyName()?.let {
            binding.header.subtitle(it)
        }

        val adapter = DeviceAlertsAdapter(this, device)
        binding.recycler.adapter = adapter

        if (device?.alerts?.firstOrNull { it.alert == DeviceAlertType.NEEDS_UPDATE } != null) {
            analytics.trackEventPrompt(
                AnalyticsService.ParamValue.OTA_AVAILABLE.paramValue,
                AnalyticsService.ParamValue.WARN.paramValue,
                AnalyticsService.ParamValue.VIEW.paramValue
            )
        }
        adapter.submitList(device?.alerts)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.DEVICE_ALERTS, classSimpleName())
    }

    override fun onUpdateStationClicked() {
        analytics.trackEventPrompt(
            AnalyticsService.ParamValue.OTA_AVAILABLE.paramValue,
            AnalyticsService.ParamValue.WARN.paramValue,
            AnalyticsService.ParamValue.ACTION.paramValue
        )
        navigator.showDeviceHeliumOTA(this, device, false)
        finish()
    }

    override fun onContactSupportClicked() {
        navigator.openSupportCenter(this, AnalyticsService.ParamValue.STATION_OFFLINE.paramValue)
        finish()
    }

    override fun onLowBatteryReadMoreClicked() {
        val url = when (device?.profile) {
            DeviceProfile.M5 -> getString(R.string.docs_url_low_battery_m5)
            DeviceProfile.D1 -> getString(R.string.docs_url_low_battery_d1)
            DeviceProfile.Helium -> getString(R.string.docs_url_low_battery_helium)
            else -> String.empty()
        }
        navigator.openWebsite(this, url)
        analytics.trackEventSelectContent(
            AnalyticsService.ParamValue.WEB_DOCUMENTATION.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, url)
        )
    }
}
