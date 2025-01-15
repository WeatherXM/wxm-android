package com.weatherxm.ui.devicesettings.helium

import android.app.Activity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import coil3.ImageLoader
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityDeviceSettingsHeliumBinding
import com.weatherxm.service.workers.UploadPhotoWorker
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.RewardSplitStakeholderAdapter
import com.weatherxm.ui.common.RewardSplitsData
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.applyOnGlobalLayout
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.loadImage
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.parcelableList
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.devicesettings.DeviceInfoItemAdapter
import com.weatherxm.ui.devicesettings.FriendlyNameDialogFragment
import com.weatherxm.util.MapboxUtils.getMinimap
import net.gotev.uploadservice.extensions.getCancelUploadIntent
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class DeviceSettingsHeliumActivity : BaseActivity() {
    private val model: DeviceSettingsHeliumViewModel by viewModel {
        parametersOf(intent.parcelable<UIDevice>(ARG_DEVICE))
    }
    private lateinit var binding: ActivityDeviceSettingsHeliumBinding
    private val imageLoader: ImageLoader by inject()
    private lateinit var adapter: DeviceInfoItemAdapter

    // Register the launcher for the edit location activity and wait for a possible result
    private val editLocationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val device = it.data?.parcelable<UIDevice>(ARG_DEVICE)
            if (it.resultCode == Activity.RESULT_OK && device != null) {
                model.device = device
                setupStationLocation(true)
            }
        }

    // Register the launcher for the photo gallery activity and wait for a possible result
    private val photoGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            /**
             * Some changes happened in the photos so we need to fetch them again or delete all of
             * them if the user left the screen with <2 photos left.
             */
            val shouldDeleteAllPhotos =
                it.data?.getBooleanExtra(Contracts.ARG_DELETE_ALL_PHOTOS, false)
            val photos = it.data?.parcelableList<StationPhoto>(Contracts.ARG_PHOTOS)
            if (it.resultCode == Activity.RESULT_OK) {
                model.onPhotosChanged(shouldDeleteAllPhotos, photos)
            }
        }

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

        model.onLoading().observe(this) {
            if (it) {
                binding.progress.visible(true)
            } else {
                binding.progress.invisible()
            }
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
            onChangeStationName()
        }

        binding.removeStationBtn.setOnClickListener {
            onRemoveStation()
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

        setupStationLocation(false)
    }

    private fun onPhotos(devicePhotos: List<String>) {
        binding.devicePhotosCard.updateUI(devicePhotos)
        binding.devicePhotosCard.setOnClickListener(
            onClick = {
                analytics.trackEventSelectContent(
                    contentType = AnalyticsService.ParamValue.GO_TO_PHOTO_VERIFICATION.paramValue,
                    Pair(
                        FirebaseAnalytics.Param.SOURCE,
                        AnalyticsService.ParamValue.SETTINGS.paramValue
                    )
                )
                val photos = arrayListOf<String>()
                devicePhotos.forEach {
                    photos.add(it)
                }
                if (photos.isEmpty()) {
                    navigator.showPhotoVerificationIntro(this, model.device)
                } else {
                    navigator.showPhotoGallery(
                        photoGalleryLauncher,
                        this,
                        model.device,
                        photos,
                        false
                    )
                }
            },
            onCancelUpload = {
                ActionDialogFragment
                    .Builder(
                        title = getString(R.string.cancel_upload),
                        message = getString(R.string.cancel_upload_message),
                        negative = getString(R.string.action_back)
                    )
                    .onPositiveClick(getString(R.string.yes_cancel)) {
                        analytics.trackEventUserAction(
                            AnalyticsService.ParamValue.CANCEL_UPLOADING_PHOTOS.paramValue
                        )
                        // Trigger a refresh on the photos through the API
                        UploadPhotoWorker.cancelWorkers(this, model.device.id)
                        model.getDevicePhotoUploadIds().onEach {
                            getCancelUploadIntent(it).send()
                        }
                        model.onPhotosChanged(false, null)
                    }
                    .build()
                    .show(this)
            },
            onRetry = {
                model.retryPhotoUpload()
                analytics.trackEventUserAction(
                    AnalyticsService.ParamValue.RETRY_UPLOADING_PHOTOS.paramValue
                )
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

    private fun setupStationLocation(forceUpdateMinimap: Boolean) {
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
            binding.locationMinimap.loadImage(imageLoader, it.toString())
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
        binding.deleteStationCard.visible(model.device.isOwned())
        if (!model.device.isOwned()) {
            binding.frequencyTitle.visible(false)
            binding.frequencyDesc.visible(false)
            binding.changeFrequencyBtn.visible(false)
            binding.rebootStationContainer.visible(false)
            binding.dividerBelowFrequency.visible(false)
            binding.dividerBelowStationName.visible(false)
        } else {
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
                analytics.trackEventPrompt(
                    AnalyticsService.ParamValue.LOW_BATTERY.paramValue,
                    AnalyticsService.ParamValue.WARN.paramValue,
                    AnalyticsService.ParamValue.VIEW.paramValue,
                    Pair(FirebaseAnalytics.Param.ITEM_ID, model.device.id)
                )
            }
            if (deviceInfo.default.any { it.deviceAlert?.alert == DeviceAlertType.NEEDS_UPDATE }) {
                analytics.trackEventPrompt(
                    AnalyticsService.ParamValue.OTA_AVAILABLE.paramValue,
                    AnalyticsService.ParamValue.WARN.paramValue,
                    AnalyticsService.ParamValue.VIEW.paramValue
                )
            }

            handleSplitRewards(deviceInfo.rewardSplit)

            adapter.submitList(deviceInfo.default)

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

    private fun handleSplitRewards(data: RewardSplitsData?) {
        if (data?.hasSplitRewards() == true) {
            binding.rewardSplitCard.visible(true)
            binding.rewardSplitDesc.text = getString(R.string.reward_split_desc, data.splits.size)
            val rewardSplitAdapter = RewardSplitStakeholderAdapter(data.wallet, true)
            binding.recyclerRewardSplit.adapter = rewardSplitAdapter
            rewardSplitAdapter.submitList(data.splits)

            val stakeHolderValue = if (model.isStakeholder(data)) {
                AnalyticsService.ParamValue.STAKEHOLDER_LOWERCASE.paramValue
            } else {
                AnalyticsService.ParamValue.NON_STAKEHOLDER.paramValue
            }
            trackRewardSplitViewContent(
                AnalyticsService.ParamValue.REWARD_SPLITTING.paramValue,
                stakeHolderValue
            )
        } else {
            val stakeHolderValue = if (model.device.isOwned()) {
                AnalyticsService.ParamValue.STAKEHOLDER_LOWERCASE.paramValue
            } else {
                AnalyticsService.ParamValue.NON_STAKEHOLDER.paramValue
            }
            trackRewardSplitViewContent(
                AnalyticsService.ParamValue.NO_REWARD_SPLITTING.paramValue,
                stakeHolderValue
            )
        }
    }

    private fun trackRewardSplitViewContent(deviceState: String, userState: String) {
        analytics.trackEventViewContent(
            AnalyticsService.ParamValue.REWARD_SPLITTING_DEVICE_SETTINGS.paramValue,
            contentId = null,
            Pair(AnalyticsService.CustomParam.DEVICE_STATE.paramName, deviceState),
            Pair(AnalyticsService.CustomParam.USER_STATE.paramName, userState)
        )
    }
}
