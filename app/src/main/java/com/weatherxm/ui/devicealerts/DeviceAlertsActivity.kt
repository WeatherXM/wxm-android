package com.weatherxm.ui.devicealerts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.databinding.ActivityDeviceAlertsBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.UserDevice
import com.weatherxm.ui.common.toast
import com.weatherxm.util.Analytics
import com.weatherxm.util.applyInsets
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class DeviceAlertsActivity : AppCompatActivity(), KoinComponent, DeviceAlertListener {
    private lateinit var binding: ActivityDeviceAlertsBinding
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()

    private var userDevice: UserDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        userDevice = intent?.extras?.getParcelable(ARG_DEVICE)
        if (userDevice == null) {
            Timber.d("Could not start DeviceAlertsActivity. UserDevice is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        with(binding.toolbar) {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
            subtitle = userDevice?.device?.getNameOrLabel()
        }

        val adapter = DeviceAlertsAdapter(this)
        binding.recycler.adapter = adapter

        adapter.submitList(userDevice?.alerts)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.DEVICE_ALERTS,
            DeviceAlertsActivity::class.simpleName
        )
    }

    override fun onUpdateStationClicked() {
        navigator.showDeviceHeliumOTA(this, userDevice?.device, false)
        finish()
    }

    override fun onContactSupportClicked() {
        navigator.sendSupportEmail(
            this,
            subject = getString(R.string.support_email_subject_device_offline),
            body = getString(R.string.support_email_body_device_name, userDevice?.device?.name)
        )
        finish()
    }
}
