package com.weatherxm.ui.devicesettings.wifi

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import coil.ImageLoader
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityDeviceSettingsWifiBinding
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.applyOnGlobalLayout
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.loadImage
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.devicesettings.DeviceInfoItemAdapter
import com.weatherxm.ui.devicesettings.FriendlyNameDialogFragment
import com.weatherxm.util.MapboxUtils.getMinimap
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class DeviceSettingsWifiActivity : BaseActivity() {
    private val model: DeviceSettingsWifiViewModel by viewModel {
        parametersOf(intent.parcelable<UIDevice>(ARG_DEVICE))
    }
    private lateinit var binding: ActivityDeviceSettingsWifiBinding
    private val imageLoader: ImageLoader by inject()
    private lateinit var defaultAdapter: DeviceInfoItemAdapter
    private lateinit var gatewayAdapter: DeviceInfoItemAdapter
    private lateinit var stationAdapter: DeviceInfoItemAdapter

    // Register the launcher for the edit location activity and wait for a possible result
    private val editLocationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val device = it.data?.parcelable<UIDevice>(ARG_DEVICE)
            if (it.resultCode == Activity.RESULT_OK && device != null) {
                model.device = device
                setupStationLocation(true)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceSettingsWifiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (model.device.isEmpty()) {
            Timber.d("Could not start DeviceSettingsActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        setupRecyclers()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupInfo()

        model.onLoading().observe(this) {
            if (it) {
                binding.progress.visibility = View.VISIBLE
            } else {
                binding.progress.visibility = View.INVISIBLE
            }
        }

        model.onEditNameChange().observe(this) {
            binding.stationName.text = it
        }

        model.onError().observe(this) {
            binding.progress.visibility = View.INVISIBLE
            showSnackbarMessage(binding.root, it.errorMessage, it.retryFunction)
        }

        model.onDeviceRemoved().observe(this) {
            navigator.showHome(this)
            finish()
        }

        binding.changeStationNameBtn.setOnClickListener {
            onChangeStationName()
        }

        binding.removeStationBtn.setOnClickListener {
            onRemoveStation()
        }

        binding.contactSupportBtn.setOnClickListener {
            navigator.openSupportCenter(this, AnalyticsService.ParamValue.DEVICE_INFO.paramValue)
        }

        if (model.device.relation == DeviceRelation.FOLLOWED) {
            binding.contactSupportBtn.text = getString(R.string.see_something_wrong_contact_support)
        }

        setupStationLocation()
    }

    private fun setupRecyclers() {
        defaultAdapter = DeviceInfoItemAdapter(null)
        gatewayAdapter = DeviceInfoItemAdapter(null)
        stationAdapter = DeviceInfoItemAdapter(null)
        binding.recyclerDefaultInfo.adapter = defaultAdapter
        binding.recyclerGatewayInfo.adapter = gatewayAdapter
        binding.recyclerStationInfo.adapter = stationAdapter
    }

    private fun setupStationLocation(forceUpdateMinimap: Boolean = false) {
        binding.editLocationBtn.setOnClickListener {
            navigator.showEditLocation(editLocationLauncher, this, model.device)
        }
        binding.editLocationBtn.visible(model.device.isOwned())

        if (model.device.relation == DeviceRelation.FOLLOWED) {
            binding.locationDesc.setHtml(
                R.string.station_location_favorite_desc, model.device.address ?: String.empty()
            )
        } else {
            binding.locationDesc.setHtml(
                R.string.station_location_desc,
                model.device.address ?: String.empty()
            )
        }

        if (forceUpdateMinimap) {
            updateMinimap()
        } else {
            binding.locationLayout.applyOnGlobalLayout {
                updateMinimap()
            }
        }
    }

    private fun updateMinimap() {
        val deviceMapLocation = if (model.device.isOwned()) {
            model.device.location
        } else {
            null
        }
        getMinimap(binding.locationLayout.width, deviceMapLocation, model.device.hex7)?.let {
            binding.locationMinimap.loadImage(imageLoader, it)
        } ?: binding.locationMinimap.visible(false)
    }

    private fun onRemoveStation() {
        analytics.trackEventSelectContent(
            AnalyticsService.ParamValue.REMOVE_DEVICE.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, model.device.id)
        )
        navigator.showPasswordPrompt(this, R.string.remove_station_password_message) {
            if (it) {
                Timber.d("Password confirmation success!")
                model.removeDevice()
            } else {
                Timber.d("Password confirmation prompt was cancelled or failed.")
            }
        }
    }

    private fun onChangeStationName() {
        FriendlyNameDialogFragment(model.device.friendlyName, model.device.id) {
            model.setOrClearFriendlyName(it)
        }.show(this)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.STATION_SETTINGS, classSimpleName())
    }

    private fun setupInfo() {
        binding.stationName.text = model.device.getDefaultOrFriendlyName()
        if (model.device.isOwned()) {
            with(binding.removeStationDesc) {
                movementMethod =
                    me.saket.bettermovementmethod.BetterLinkMovementMethod.newInstance().apply {
                        setOnLinkClickListener { _, url ->
                            navigator.openWebsite(context, url)
                            return@setOnLinkClickListener true
                        }
                    }
                setHtml(
                    R.string.remove_station_desc,
                    getString(R.string.docs_url)
                )
            }
        } else {
            binding.deleteStationCard.visible(false)
        }

        model.onDeviceInfo().observe(this) { deviceInfo ->
            if (deviceInfo.station.any { it.warning != null }) {
                analytics.trackEventPrompt(
                    AnalyticsService.ParamValue.LOW_BATTERY.paramValue,
                    AnalyticsService.ParamValue.WARN.paramValue,
                    AnalyticsService.ParamValue.VIEW.paramValue,
                    Pair(FirebaseAnalytics.Param.ITEM_ID, model.device.id)
                )
            }
            defaultAdapter.submitList(deviceInfo.default)
            gatewayAdapter.submitList(deviceInfo.gateway)
            stationAdapter.submitList(deviceInfo.station)

            binding.shareBtn.setOnClickListener {
                navigator.openShare(this, model.parseDeviceInfoToShare(deviceInfo))

                analytics.trackEventUserAction(
                    actionName = AnalyticsService.ParamValue.SHARE_STATION_INFO.paramValue,
                    contentType = AnalyticsService.ParamValue.STATION_INFO.paramValue,
                    Pair(FirebaseAnalytics.Param.ITEM_ID, model.device.id)
                )
            }
        }

        model.getDeviceInformation(this)
    }
}
