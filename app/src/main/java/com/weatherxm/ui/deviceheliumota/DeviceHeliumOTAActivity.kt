package com.weatherxm.ui.deviceheliumota

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.databinding.ActivityHeliumOtaBinding
import com.weatherxm.ui.common.Contracts.ARG_BLE_DEVICE_CONNECTED
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_NEEDS_PHOTO_VERIFICATION
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseActivity
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class DeviceHeliumOTAActivity : BaseActivity() {
    private lateinit var binding: ActivityHeliumOtaBinding
    private val bluetoothAdapter: BluetoothAdapter? by inject()

    private val model: DeviceHeliumOTAViewModel by viewModel {
        parametersOf(
            intent.parcelable<UIDevice>(ARG_DEVICE),
            intent.getBooleanExtra(ARG_BLE_DEVICE_CONNECTED, false),
            intent.getBooleanExtra(ARG_NEEDS_PHOTO_VERIFICATION, false)
        )
    }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                requestBluetoothPermissions(
                    onGranted = { model.startConnectionProcess() },
                    onDenied = { binding.bleActionFlow.onError(true, R.string.no_bluetooth_access) }
                )
            } else {
                binding.bleActionFlow.onError(true, R.string.bluetooth_not_enabled)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeliumOtaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (model.device.isEmpty()) {
            Timber.d("Could not start DeviceHeliumOTAActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        with(binding.toolbar) {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
            subtitle = model.device.name
        }

        setListeners()

        model.onStatus().observe(this) {
            onNewStatus(it)
        }

        model.onInstallingProgress().observe(this) {
            binding.bleActionFlow.onProgressChanged(it)
        }

        binding.bleActionFlow.onShowStationUpdateMetadata(
            model.device.getDefaultOrFriendlyName(),
            "${model.device.currentFirmware} ➞ ${model.device.assignedFirmware}"
        )

        initBluetoothAndStart()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.HELIUM_OTA, classSimpleName())
    }

    override fun onDestroy() {
        model.disconnectFromPeripheral()
        super.onDestroy()
    }

    private fun setListeners() {
        binding.photoVerificationBtn.setOnClickListener {
            navigator.showPhotoVerificationIntro(this)
            finish()
        }
        binding.skipAndGoToStationBtn.setOnClickListener {
            ActionDialogFragment.createSkipPhotoVerification(this) {
                navigator.showDeviceDetails(this, device = model.device)
                finish()
            }.show(this)
        }
        binding.bleActionFlow.setListeners(onScanClicked = {
            analytics.trackEventSelectContent(AnalyticsService.ParamValue.BLE_SCAN_AGAIN.paramValue)
            initBluetoothAndStart()
        }, onPairClicked = {
            lifecycleScope.launch {
                model.connect(true)
            }
        }, onSuccessPrimaryButtonClicked = {
            navigator.showDeviceDetails(this, device = model.device)
            finish()
        }, onCancelButtonClicked = {
            finish()
        }, onRetryButtonClicked = {
            model.startConnectionProcess()
        })
    }

    private fun onNewStatus(it: Resource<State>) {
        when (it.status) {
            Status.SUCCESS -> {
                if (model.needsPhotoVerification) {
                    binding.bleActionFlow.onSuccess(
                        title = R.string.station_updated,
                        message = getString(R.string.station_updated_subtitle),
                        successActionText = null
                    )
                    binding.successWithPhotoVerificationContainer.visible(true)
                } else {
                    binding.bleActionFlow.onSuccess(
                        title = R.string.station_updated,
                        message = getString(R.string.station_updated_subtitle),
                        successActionText = getString(R.string.action_view_station)
                    )
                }
                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.OTA_RESULT.paramValue,
                    contentId = AnalyticsService.ParamValue.OTA_RESULT_ID.paramValue,
                    Pair(FirebaseAnalytics.Param.ITEM_ID, model.device.id),
                    success = 1L
                )
            }
            Status.LOADING -> {
                onLoadingStatusUpdate(it.data?.status)
            }
            Status.ERROR -> {
                onErrorStatusUpdate(it)
                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.OTA_RESULT.paramValue,
                    contentId = AnalyticsService.ParamValue.OTA_RESULT_ID.paramValue,
                    Pair(FirebaseAnalytics.Param.ITEM_ID, model.device.id),
                    success = 0L
                )
            }
        }
    }

    private fun onErrorStatusUpdate(resource: Resource<State>) {
        when (resource.data?.status) {
            OTAStatus.SCAN_FOR_STATION -> {
                val title = if (resource.data.failure is BluetoothError.DeviceNotFound) {
                    R.string.station_not_in_range
                } else {
                    R.string.scan_failed_title
                }
                binding.bleActionFlow.onError(true, title, message = resource.message)
            }
            OTAStatus.PAIR_STATION -> {
                binding.bleActionFlow.onNotPaired()
            }
            OTAStatus.CONNECT_TO_STATION, OTAStatus.DOWNLOADING, OTAStatus.INSTALLING -> {
                val errorCode = if (resource.data.otaError != null) {
                    getString(
                        R.string.error_helium_ota_failed,
                        resource.data.otaError,
                        resource.data.otaErrorType,
                        resource.data.otaErrorMessage
                    )
                } else {
                    resource.message
                }
                binding.bleActionFlow.onError(
                    false,
                    R.string.update_failed,
                    getString(R.string.action_retry_updating),
                    getString(R.string.update_failed_message),
                    errorCode
                ) {
                    navigator.openSupportCenter(this)
                }
            }
            else -> {
                toast(R.string.error_reach_out_short)
            }
        }

        analytics.trackEventViewContent(
            contentName = AnalyticsService.ParamValue.OTA_ERROR.paramValue,
            contentId = AnalyticsService.ParamValue.OTA_ERROR_ID.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, model.device.id),
            Pair(
                AnalyticsService.CustomParam.STEP.paramName,
                when (resource.data?.status) {
                    OTAStatus.SCAN_FOR_STATION -> AnalyticsService.ParamValue.SCAN.paramValue
                    OTAStatus.PAIR_STATION -> AnalyticsService.ParamValue.PAIR.paramValue
                    OTAStatus.CONNECT_TO_STATION -> AnalyticsService.ParamValue.CONNECT.paramValue
                    OTAStatus.DOWNLOADING -> AnalyticsService.ParamValue.DOWNLOAD.paramValue
                    OTAStatus.INSTALLING -> AnalyticsService.ParamValue.INSTALL.paramValue
                    null -> String.empty()
                }
            )
        )
    }

    private fun onLoadingStatusUpdate(status: OTAStatus?) {
        with(binding.bleActionFlow) {
            when (status) {
                OTAStatus.CONNECT_TO_STATION -> {
                    onStep(0, R.string.connecting_to_station, showFirmwareInfo = true)
                }
                OTAStatus.DOWNLOADING -> {
                    onStep(
                        1,
                        R.string.downloading_update,
                        R.string.downloading_update_subtitle,
                        showFirmwareInfo = true
                    )
                }
                else -> {
                    onStep(
                        2,
                        R.string.installing_update,
                        R.string.installing_update_subtitle,
                        showProgressBar = true,
                        showFirmwareInfo = true
                    )
                }
            }
        }
    }

    private fun initBluetoothAndStart() {
        bluetoothAdapter?.let {
            if (it.isEnabled) {
                requestBluetoothPermissions(
                    onGranted = { model.startConnectionProcess() },
                    onDenied = { binding.bleActionFlow.onError(true, R.string.no_bluetooth_access) }
                )
            } else {
                requestToEnableBluetooth(
                    onGranted = { navigator.showBluetoothEnablePrompt(enableBluetoothLauncher) },
                    onDenied = {
                        binding.bleActionFlow.onError(true, R.string.no_bluetooth_access)
                    }
                )
            }
        } ?: run { binding.bleActionFlow.onError(true, R.string.no_bluetooth_available) }
    }
}
