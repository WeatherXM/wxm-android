package com.weatherxm.ui.devicealerts

import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.databinding.ActivityDeviceAlertsBinding
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.Analytics
import timber.log.Timber

class DeviceAlertsActivity : BaseActivity(), DeviceAlertListener {
    private lateinit var binding: ActivityDeviceAlertsBinding

    private var device: UIDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        device = intent?.extras?.parcelable(ARG_DEVICE)
        if (device == null) {
            Timber.d("Could not start DeviceAlertsActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        with(binding.toolbar) {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
            subtitle = device?.getDefaultOrFriendlyName()
        }

        val adapter = DeviceAlertsAdapter(this, device)
        binding.recycler.adapter = adapter

        if (device?.alerts?.contains(DeviceAlert.NEEDS_UPDATE) == true) {
            analytics.trackEventPrompt(
                Analytics.ParamValue.OTA_AVAILABLE.paramValue,
                Analytics.ParamValue.WARN.paramValue,
                Analytics.ParamValue.VIEW.paramValue
            )
        }
        adapter.submitList(device?.alerts)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Analytics.Screen.DEVICE_ALERTS, this::class.simpleName)
    }

    override fun onUpdateStationClicked() {
        analytics.trackEventPrompt(
            Analytics.ParamValue.OTA_AVAILABLE.paramValue,
            Analytics.ParamValue.WARN.paramValue,
            Analytics.ParamValue.ACTION.paramValue
        )
        navigator.showDeviceHeliumOTA(this, device, false)
        finish()
    }

    override fun onContactSupportClicked() {
        navigator.openSupportCenter(this, Analytics.ParamValue.DEVICE_ALERTS.paramValue)
        finish()
    }
}
