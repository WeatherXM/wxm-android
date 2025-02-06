package com.weatherxm.ui.devicesettings.helium

import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityDeviceSettingsHeliumBinding
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
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.devicesettings.BaseDeviceSettingsActivity
import com.weatherxm.ui.devicesettings.DeviceInfoItemAdapter
import net.gotev.uploadservice.extensions.getCancelUploadIntent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class DeviceSettingsHeliumActivity : BaseDeviceSettingsActivity() {
    private val model: DeviceSettingsHeliumViewModel by viewModel {
        parametersOf(intent.parcelable<UIDevice>(ARG_DEVICE))
    }
    private lateinit var binding: ActivityDeviceSettingsHeliumBinding
    private lateinit var adapter: DeviceInfoItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceSettingsHeliumBinding.inflate(layoutInflater)
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

        binding.changeFrequencyBtn.setOnClickListener {
            navigator.showChangeFrequency(this, model.device)
        }

        binding.rebootStationBtn.setOnClickListener {
            navigator.showRebootStation(this, model.device)
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
        adapter = DeviceInfoItemAdapter {
            if (it.alert == DeviceAlertType.NEEDS_UPDATE) {
                analytics.trackEventPrompt(
                    AnalyticsService.ParamValue.OTA_AVAILABLE.paramValue,
                    AnalyticsService.ParamValue.WARN.paramValue,
                    AnalyticsService.ParamValue.ACTION.paramValue
                )
                navigator.showDeviceHeliumOTA(this, model.device)
                finish()
            }
        }
        binding.recyclerInfo.adapter = adapter
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
            binding.frequencyTitle.visible(false)
            binding.frequencyDesc.visible(false)
            binding.changeFrequencyBtn.visible(false)
            binding.rebootStationContainer.visible(false)
            binding.dividerBelowFrequency.visible(false)
            binding.dividerBelowStationName.visible(false)
            binding.deleteStationCard.visible(false)
        }

        with(binding.frequencyDesc) {
            movementMethod =
                me.saket.bettermovementmethod.BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, url ->
                        navigator.openWebsite(context, url)
                        return@setOnLinkClickListener true
                    }
                }
            setHtml(
                R.string.change_station_frequency_helium,
                getString(R.string.helium_frequencies_mapping_url)
            )
        }

        model.onDeviceInfo().observe(this) { deviceInfo ->
            if (deviceInfo.default.any { it.deviceAlert?.alert == DeviceAlertType.LOW_BATTERY }) {
                trackLowBatteryWarning(model.device.id)
            }
            if (deviceInfo.default.any { it.deviceAlert?.alert == DeviceAlertType.NEEDS_UPDATE }) {
                analytics.trackEventPrompt(
                    AnalyticsService.ParamValue.OTA_AVAILABLE.paramValue,
                    AnalyticsService.ParamValue.WARN.paramValue,
                    AnalyticsService.ParamValue.VIEW.paramValue
                )
            }

            handleRewardSplits(
                binding.rewardSplitCard,
                binding.rewardSplitDesc,
                binding.recyclerRewardSplit,
                deviceInfo.rewardSplit,
                model.isStakeholder(deviceInfo.rewardSplit),
                model.device.isOwned()
            )

            adapter.submitList(deviceInfo.default)

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
