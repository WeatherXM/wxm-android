package com.weatherxm.ui.devicesettings.changefrequency

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityChangeFrequencyStationBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.hide
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.devicesettings.ChangeFrequencyState
import com.weatherxm.ui.devicesettings.FrequencyStatus
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class ChangeFrequencyActivity : BaseActivity() {
    private lateinit var binding: ActivityChangeFrequencyStationBinding
    private val bluetoothAdapter: BluetoothAdapter? by inject()

    private val model: ChangeFrequencyViewModel by viewModel {
        parametersOf(intent.parcelable<UIDevice>(Contracts.ARG_DEVICE))
    }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                requestBluetoothPermissions(
                    onGranted = { model.startConnectionProcess() },
                    onDenied = {
                        binding.bleActionFlow.onError(true, R.string.no_bluetooth_access)
                    }
                )
            } else {
                binding.bleActionFlow.onError(true, R.string.bluetooth_not_enabled)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeFrequencyStationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (model.device.isEmpty()) {
            Timber.d("Could not start ChangeFrequencyActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        onBackPressedDispatcher.addCallback {
            model.disconnectFromPeripheral()
            finishActivity()
        }

        with(binding.toolbar) {
            setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            subtitle = model.device.name
        }

        setListeners()

        model.onFrequencies().observe(this) { result ->
            binding.setFrequencyView.defaultState(result, false)
        }

        model.onStatus().observe(this) {
            onNewStatus(it)
        }

        model.getCountryAndFrequencies()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            AnalyticsService.Screen.CHANGE_STATION_FREQUENCY, classSimpleName(), model.device.id
        )
    }

    private fun finishActivity() {
        model.stopScanning()
        finish()
    }

    private fun setListeners() {
        binding.setFrequencyView.listener(
            onFrequencyDocumentation = {
                analytics.trackEventSelectContent(
                    AnalyticsService.ParamValue.DOCUMENTATION_FREQUENCY.paramValue
                )
                navigator.openWebsite(this, it)
            },
            onBack = {
                analytics.trackEventUserAction(
                    actionName = AnalyticsService.ParamValue.CHANGE_FREQUENCY_RESULT.paramValue,
                    contentType = AnalyticsService.ParamValue.CHANGE_FREQUENCY.paramValue,
                    Pair(
                        AnalyticsService.CustomParam.ACTION.paramName,
                        AnalyticsService.ParamValue.CANCEL.paramValue
                    )
                )
                model.disconnectFromPeripheral()
                finishActivity()
            },
            onSet = {
                analytics.trackEventUserAction(
                    actionName = AnalyticsService.ParamValue.CHANGE_FREQUENCY_RESULT.paramValue,
                    contentType = AnalyticsService.ParamValue.CHANGE_FREQUENCY.paramValue,
                    Pair(
                        AnalyticsService.CustomParam.ACTION.paramName,
                        AnalyticsService.ParamValue.CHANGE.paramValue
                    )
                )
                model.setSelectedFrequency(it)
                initBluetoothAndStart()
                binding.setFrequencyView.hide(null)
                binding.bleActionFlow.setVisible(true)
            }
        )

        binding.bleActionFlow.setListeners(onScanClicked = {
            analytics.trackEventSelectContent(AnalyticsService.ParamValue.BLE_SCAN_AGAIN.paramValue)
            initBluetoothAndStart()
        }, onPairClicked = {
            lifecycleScope.launch {
                model.connect(true)
            }
        }, onSuccessPrimaryButtonClicked = {
            finishActivity()
        }, onCancelButtonClicked = {
            model.disconnectFromPeripheral()
            finishActivity()
        }, onRetryButtonClicked = {
            model.startConnectionProcess()
        })
    }

    private fun onNewStatus(it: Resource<ChangeFrequencyState>) {
        when (it.status) {
            Status.SUCCESS -> {
                binding.bleActionFlow.onSuccess(
                    title = R.string.frequency_changed,
                    message = getString(
                        R.string.frequency_changed_subtitle,
                        model.getSelectedFrequency()
                    ),
                    primaryActionText = getString(R.string.back_to_settings)
                )
                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.CHANGE_FREQUENCY_RESULT.paramValue,
                    contentId = AnalyticsService.ParamValue.CHANGE_FREQUENCY_RESULT_ID.paramValue,
                    success = 1L
                )
            }
            Status.LOADING -> {
                onLoadingStatusUpdate(it.data?.status)
            }
            Status.ERROR -> {
                onErrorStatusUpdate(it)
                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.CHANGE_FREQUENCY_RESULT.paramValue,
                    contentId = AnalyticsService.ParamValue.CHANGE_FREQUENCY_RESULT_ID.paramValue,
                    success = 0L
                )
            }
        }
    }

    private fun onErrorStatusUpdate(resource: Resource<ChangeFrequencyState>) {
        when (resource.data?.status) {
            FrequencyStatus.SCAN_FOR_STATION -> {
                val title = if (resource.data.failure is BluetoothError.DeviceNotFound) {
                    R.string.station_not_in_range
                } else {
                    R.string.scan_failed_title
                }
                binding.bleActionFlow.onError(true, title, message = resource.message)
            }
            FrequencyStatus.PAIR_STATION -> {
                binding.bleActionFlow.onNotPaired()
            }
            FrequencyStatus.CONNECT_TO_STATION, FrequencyStatus.CHANGING_FREQUENCY -> {
                binding.bleActionFlow.onError(
                    false,
                    R.string.frequency_changed_failed,
                    getString(R.string.action_retry),
                    getString(R.string.changing_frequency_failed_message),
                    resource.message
                ) {
                    navigator.openSupportCenter(this)
                }
            }
            else -> {
                toast(R.string.error_reach_out_short)
            }
        }
    }

    private fun onLoadingStatusUpdate(status: FrequencyStatus?) {
        with(binding.bleActionFlow) {
            when (status) {
                FrequencyStatus.CONNECT_TO_STATION -> {
                    onStep(0, R.string.connecting_to_station)
                }
                FrequencyStatus.CHANGING_FREQUENCY -> {
                    onStep(1, R.string.changing_frequency)
                }
                else -> {
                    onStep(0, R.string.connecting_to_station)
                }
            }
        }
    }

    private fun initBluetoothAndStart() {
        bluetoothAdapter?.let {
            if (it.isEnabled) {
                requestBluetoothPermissions(
                    onGranted = { model.startConnectionProcess() },
                    onDenied = {
                        binding.bleActionFlow.onError(true, R.string.no_bluetooth_access)
                    }
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
