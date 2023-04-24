package com.weatherxm.ui.deviceheliumota

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityHeliumOtaBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts.ARG_BLE_DEVICE_CONNECTED
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.common.toast
import com.weatherxm.util.applyInsets
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class DeviceHeliumOTAActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityHeliumOtaBinding
    private val bluetoothAdapter: BluetoothAdapter? by inject()
    private val navigator: Navigator by inject()

    private val model: DeviceHeliumOTAViewModel by viewModel {
        parametersOf(
            intent.getParcelableExtra<Device>(ARG_DEVICE),
            intent.getBooleanExtra(ARG_BLE_DEVICE_CONNECTED, false)
        )
    }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                startScan()
            } else {
                binding.bleActionFlow.onError(true, R.string.bluetooth_not_enabled)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeliumOtaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

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

        val currentFirmwareVersion = model.device.attributes?.firmware?.current
        val assignedFirmwareVersion = model.device.attributes?.firmware?.assigned
        binding.bleActionFlow.onShowStationUpdateMetadata(
            model.device.getNameOrLabel(),
            "$currentFirmwareVersion ➞ $assignedFirmwareVersion"
        )

        initBluetoothAndStart()
    }

    override fun onDestroy() {
        model.disconnectFromPeripheral()
        super.onDestroy()
    }

    private fun setListeners() {
        binding.bleActionFlow.setListeners(onScanClicked = {
            initBluetoothAndStart()
        }, onPairClicked = {
            model.pairDevice()
        }, onSuccessPrimaryButtonClicked = {
            navigator.showUserDevice(this, model.device)
            finish()
        }, onCancelButtonClicked = {
            finish()
        }, onRetryButtonClicked = {
            model.setPeripheral()
        })
    }

    private fun onNewStatus(it: Resource<State>) {
        when (it.status) {
            Status.SUCCESS -> {
                binding.bleActionFlow.onSuccess(
                    title = R.string.station_updated,
                    message = getString(R.string.station_updated_subtitle),
                    primaryActionText = getString(R.string.action_view_station)
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
                    sendSupportEmail(it)
                }
            }
            else -> {
                toast(R.string.error_reach_out_short)
            }
        }
    }

    private fun onLoadingStatusUpdate(status: OTAStatus?) {
        with(binding.bleActionFlow) {
            when (status) {
                OTAStatus.CONNECT_TO_STATION -> {
                    onStep(0, R.string.connecting_to_station)
                }
                OTAStatus.DOWNLOADING -> {
                    onStep(1, R.string.downloading_update, R.string.downloading_update_subtitle)
                }
                else -> {
                    onStep(2, R.string.installing_update, R.string.installing_update_subtitle, true)
                }
            }
        }
    }

    private fun sendSupportEmail(errorCode: String?) {
        navigator.sendSupportEmail(
            this,
            recipient = getString(R.string.support_email_recipient),
            subject = getString(R.string.support_email_subject_helium_ota_failed),
            body = getString(
                R.string.support_email_body_helium_failed,
                model.device.name,
                errorCode ?: getString(R.string.unknown)
            )
        )
    }

    private fun initBluetoothAndStart() {
        bluetoothAdapter?.let {
            if (it.isEnabled) {
                startScan()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    checkPermissionsAndThen(permissions = arrayOf(BLUETOOTH_CONNECT),
                        rationaleTitle = getString(R.string.permission_bluetooth_title),
                        rationaleMessage = getString(R.string.perm_bluetooth_scanning_desc),
                        onGranted = {
                            navigator.showBluetoothEnablePrompt(enableBluetoothLauncher)
                        },
                        onDenied = {
                            binding.bleActionFlow.onError(true, R.string.no_bluetooth_access)
                        })
                } else {
                    navigator.showBluetoothEnablePrompt(enableBluetoothLauncher)
                }
            }
        } ?: run { binding.bleActionFlow.onError(true, R.string.no_bluetooth_available) }
    }

    private fun startScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkPermissionsAndThen(permissions = arrayOf(BLUETOOTH_SCAN, BLUETOOTH_CONNECT),
                rationaleTitle = getString(R.string.permission_bluetooth_title),
                rationaleMessage = getString(R.string.perm_bluetooth_scanning_desc),
                onGranted = { model.startScan() },
                onDenied = { binding.bleActionFlow.onError(true, R.string.no_bluetooth_access) })
        } else {
            checkPermissionsAndThen(permissions = arrayOf(
                ACCESS_FINE_LOCATION,
                ACCESS_COARSE_LOCATION
            ),
                rationaleTitle = getString(R.string.permission_location_title),
                rationaleMessage = getString(R.string.perm_location_scanning_desc),
                onGranted = { model.startScan() },
                onDenied = { binding.bleActionFlow.onError(true, R.string.no_bluetooth_access) })
        }
    }
}
