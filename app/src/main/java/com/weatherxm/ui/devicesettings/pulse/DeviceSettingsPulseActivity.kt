package com.weatherxm.ui.devicesettings.pulse

import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityDeviceSettingsBaseBinding
import com.weatherxm.service.workers.UploadPhotoWorker
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.applyOnGlobalLayout
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.devicesettings.BaseDeviceSettingsActivity
import com.weatherxm.ui.devicesettings.DeviceInfoItemAdapter
import net.gotev.uploadservice.extensions.getCancelUploadIntent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class DeviceSettingsPulseActivity : BaseDeviceSettingsActivity() {
    private val model: DeviceSettingsPulseViewModel by viewModel {
        parametersOf(intent.parcelable<UIDevice>(ARG_DEVICE))
    }
    private lateinit var binding: ActivityDeviceSettingsBaseBinding
    private lateinit var defaultAdapter: DeviceInfoItemAdapter
    private lateinit var gatewayAdapter: DeviceInfoItemAdapter
    private lateinit var stationAdapter: DeviceInfoItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceSettingsBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (model.device.isEmpty()) {
            Timber.d("Could not start DeviceSettingsPulseActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        setupRecyclers()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupInfo()

        binding.swiperefresh.setOnRefreshListener {
            model.getDeviceInformation(this)
        }

        model.onLoading().observe(this) {
            handleLoading(binding.swiperefresh, binding.progress, it)
        }

        model.onEditNameChange().observe(this) {
            binding.stationName.text = it
        }

        model.onError().observe(this) {
            binding.progress.invisible()
            showSnackbarMessage(binding.root, it.errorMessage, it.retryFunction)
        }

        model.onDeviceRemoved().observe(this) {
            navigator.showHome(this)
            finish()
        }

        model.onPhotos().observe(this) {
            onPhotos(it)
        }

        binding.changeStationNameBtn.setOnClickListener {
            onChangeStationName(model.device) {
                model.setOrClearFriendlyName(it)
            }
        }

        binding.removeStationBtn.setOnClickListener {
            onRemoveStation(model.device) {
                model.removeDevice()
            }
        }

        binding.contactSupportBtn.setOnClickListener {
            navigator.openSupportCenter(this, AnalyticsService.ParamValue.DEVICE_INFO.paramValue)
        }

        if (model.device.relation == DeviceRelation.FOLLOWED) {
            binding.contactSupportBtn.text = getString(R.string.see_something_wrong_contact_support)
        }

        binding.devicePhotosCard.initProgressView(
            device = model.device,
            onRefresh = {
                // Trigger a refresh on the photos through the API
                model.onPhotosChanged(false, null)
            }
        )

        updateStationLocation(false)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.STATION_SETTINGS, classSimpleName())
        model.getDeviceInformation(this)
    }

    private fun onPhotos(devicePhotos: List<String>) {
        binding.devicePhotosCard.updateUI(devicePhotos)
        binding.devicePhotosCard.setOnClickListener(
            onClick = {
                onPhotosClicked(devicePhotos, model.getAcceptedPhotoTerms(), model.device)
            },
            onCancelUpload = {
                onPhotosCancelPrompt {
                    // Trigger a refresh on the photos through the API
                    UploadPhotoWorker.cancelWorkers(this, model.device.id)
                    model.getDevicePhotoUploadIds().onEach {
                        getCancelUploadIntent(it).send()
                    }
                    model.onPhotosChanged(false, null)
                }
            },
            onRetry = {
                onPhotosRetry { model.retryPhotoUpload() }
            }
        )
        binding.devicePhotosCard.visible(true)
    }

    private fun setupRecyclers() {
        defaultAdapter = DeviceInfoItemAdapter(null)
        gatewayAdapter = DeviceInfoItemAdapter(null)
        stationAdapter = DeviceInfoItemAdapter {
            if (it.alert == DeviceAlertType.LOW_STATION_RSSI) {
                navigator.openWebsite(this, getString(R.string.troubleshooting_pulse_url))
                finish()
            }
        }
        binding.recyclerDefaultInfo.adapter = defaultAdapter
        binding.recyclerGatewayInfo.adapter = gatewayAdapter
        binding.recyclerStationInfo.adapter = stationAdapter
    }

    private fun updateStationLocation(forceUpdateMinimap: Boolean) {
        setupStationLocation(model.device, binding.editLocationBtn, binding.locationDesc)

        if (forceUpdateMinimap) {
            updateMinimap(model.device, binding.locationLayout, binding.locationMinimap)
        } else {
            binding.locationLayout.applyOnGlobalLayout {
                updateMinimap(model.device, binding.locationLayout, binding.locationMinimap)
            }
        }
    }

    private fun setupInfo() {
        binding.stationName.text = model.device.getDefaultOrFriendlyName()
        if (model.device.isOwned()) {
            setupRemoveStationDescription(binding.removeStationDesc)
        } else {
            binding.deleteStationCard.visible(false)
        }

        model.onDeviceInfo().observe(this) { deviceInfo ->
            if (deviceInfo.station.any {
                    it.deviceAlert?.alert == DeviceAlertType.LOW_BATTERY ||
                        it.deviceAlert?.alert == DeviceAlertType.LOW_GATEWAY_BATTERY
                }
            ) {
                trackLowBatteryWarning(model.device.id)
            }

            handleRewardSplits(
                binding.rewardSplitCard,
                binding.rewardSplitDesc,
                binding.recyclerRewardSplit,
                deviceInfo.rewardSplit,
                model.isStakeholder(deviceInfo.rewardSplit),
                model.device.isOwned()
            )

            defaultAdapter.submitList(deviceInfo.default)
            gatewayAdapter.submitList(deviceInfo.gateway)
            stationAdapter.submitList(deviceInfo.station)

            binding.shareBtn.setOnClickListener {
                onShare(model.parseDeviceInfoToShare(deviceInfo), model.device.id)
            }
        }
    }

    override fun onEditLocation(device: UIDevice) {
        model.device = device
        updateStationLocation(true)
    }

    override fun onPhotosChanged(
        shouldDeleteAllPhotos: Boolean?,
        photos: ArrayList<StationPhoto>?
    ) {
        model.onPhotosChanged(shouldDeleteAllPhotos, photos)
    }
}
