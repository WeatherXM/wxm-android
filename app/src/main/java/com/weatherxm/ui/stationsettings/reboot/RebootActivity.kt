package com.weatherxm.ui.stationsettings.reboot

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
import com.weatherxm.databinding.ActivityRebootStationBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.stationsettings.RebootState
import com.weatherxm.ui.stationsettings.RebootStatus
import com.weatherxm.util.Analytics
import com.weatherxm.util.applyInsets
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class RebootActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityRebootStationBinding
    private val bluetoothAdapter: BluetoothAdapter? by inject()
    private val analytics: Analytics by inject()
    private val navigator: Navigator by inject()

    private val model: RebootViewModel by viewModel {
        parametersOf(intent.getParcelableExtra<Device>(Contracts.ARG_DEVICE))
    }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                scanConnectAndReboot()
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

        with(binding.toolbar) {
            setNavigationOnClickListener { finishActivity() }
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
            Analytics.Screen.REBOOT_STATION,
            RebootActivity::class.simpleName,
            model.device.id
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
                    sendSupportEmail(it)
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
        model.scanningJob.cancel()
        finish()
    }

    private fun setListeners() {
        binding.bleActionFlow.setListeners(onScanClicked = {
            analytics.trackEventSelectContent(Analytics.ParamValue.BLE_SCAN_AGAIN.paramValue)
            initBluetoothAndStart()
        }, onPairClicked = {
            model.pairDevice()
        }, onSuccessPrimaryButtonClicked = {
            finishActivity()
        }, onCancelButtonClicked = {
            finishActivity()
        }, onRetryButtonClicked = {
            model.scanConnectAndReboot()
        })
    }

    private fun initBluetoothAndStart() {
        bluetoothAdapter?.let {
            if (it.isEnabled) {
                scanConnectAndReboot()
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

    private fun scanConnectAndReboot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkPermissionsAndThen(permissions = arrayOf(BLUETOOTH_SCAN, BLUETOOTH_CONNECT),
                rationaleTitle = getString(R.string.permission_bluetooth_title),
                rationaleMessage = getString(R.string.perm_bluetooth_scanning_desc),
                onGranted = { model.scanConnectAndReboot() },
                onDenied = {
                    binding.bleActionFlow.onError(true, R.string.no_bluetooth_access)
                })
        } else {
            checkPermissionsAndThen(permissions = arrayOf(
                ACCESS_FINE_LOCATION,
                ACCESS_COARSE_LOCATION
            ),
                rationaleTitle = getString(R.string.permission_location_title),
                rationaleMessage = getString(R.string.perm_location_scanning_desc),
                onGranted = { model.scanConnectAndReboot() },
                onDenied = {
                    binding.bleActionFlow.onError(true, R.string.no_bluetooth_access)
                })
        }
    }

    private fun sendSupportEmail(errorCode: String?) {
        navigator.sendSupportEmail(
            this,
            subject = getString(R.string.support_email_subject_helium_reboot_failed),
            body = getString(
                R.string.support_email_body_helium_failed,
                model.device.name,
                errorCode ?: getString(R.string.unknown)
            )
        )
    }
}
