package com.weatherxm.ui.devicesettings.reboot

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.weatherxm.R
import com.weatherxm.analytics.Analytics
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityRebootStationBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.devicesettings.RebootState
import com.weatherxm.ui.devicesettings.RebootStatus
import com.weatherxm.ui.common.getClassSimpleName
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class RebootActivity : BaseActivity() {
    private lateinit var binding: ActivityRebootStationBinding
    private val bluetoothAdapter: BluetoothAdapter? by inject()

    private val model: RebootViewModel by viewModel {
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
        binding = ActivityRebootStationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        if (model.device.isEmpty()) {
            Timber.d("Could not start RebootActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        onBackPressedDispatcher.addCallback {
            finishActivity()
        }

        with(binding.toolbar) {
            setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            subtitle = model.device.name
        }

        setListeners()

        model.onStatus().observe(this) {
            onNewStatus(it)
        }

        initBluetoothAndStart()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.REBOOT_STATION, getClassSimpleName(), model.device.id
        )
    }

    private fun onNewStatus(it: Resource<RebootState>) {
        when (it.status) {
            Status.SUCCESS -> {
                binding.bleActionFlow.onSuccess(
                    title = R.string.station_rebooted,
                    message = getString(R.string.station_rebooted_subtitle),
                    primaryActionText = getString(R.string.back_to_settings)
                )
            }
            Status.LOADING -> {
                onLoadingStatusUpdate(it.data?.status)
            }
            Status.ERROR -> {
                onErrorStatusUpdate(it)
            }
        }
    }

    private fun onErrorStatusUpdate(resource: Resource<RebootState>) {
        when (resource.data?.status) {
            RebootStatus.SCAN_FOR_STATION -> {
                val title = if (resource.data.failure is BluetoothError.DeviceNotFound) {
                    R.string.station_not_in_range
                } else {
                    R.string.scan_failed_title
                }
                binding.bleActionFlow.onError(true, title, message = resource.message)
            }
            RebootStatus.PAIR_STATION -> {
                binding.bleActionFlow.onNotPaired()
            }
            RebootStatus.CONNECT_TO_STATION, RebootStatus.REBOOTING -> {
                binding.bleActionFlow.onError(
                    false,
                    R.string.reboot_failed,
                    getString(R.string.action_retry),
                    getString(R.string.reboot_failed_message),
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

    private fun onLoadingStatusUpdate(status: RebootStatus?) {
        with(binding.bleActionFlow) {
            when (status) {
                RebootStatus.CONNECT_TO_STATION -> {
                    onStep(0, R.string.connecting_to_station)
                }
                RebootStatus.REBOOTING -> {
                    onStep(1, R.string.rebooting_station)
                }
                else -> {
                    onStep(0, R.string.connecting_to_station)
                }
            }
        }
    }

    private fun finishActivity() {
        model.stopScanning()
        finish()
    }

    private fun setListeners() {
        binding.bleActionFlow.setListeners(onScanClicked = {
            analytics.trackEventSelectContent(Analytics.ParamValue.BLE_SCAN_AGAIN.paramValue)
            initBluetoothAndStart()
        }, onPairClicked = {
            lifecycleScope.launch {
                model.connect(true)
            }
        }, onSuccessPrimaryButtonClicked = {
            finishActivity()
        }, onCancelButtonClicked = {
            finishActivity()
        }, onRetryButtonClicked = {
            model.startConnectionProcess()
        })
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
