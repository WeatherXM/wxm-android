package com.weatherxm.ui.stationsettings.changefrequency

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityChangeFrequencyStationBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.common.hide
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.stationsettings.ChangeFrequencyState
import com.weatherxm.ui.stationsettings.FrequencyStatus
import com.weatherxm.util.Analytics
import com.weatherxm.util.applyInsets
import com.weatherxm.util.setHtml
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class ChangeFrequencyActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityChangeFrequencyStationBinding
    private val bluetoothAdapter: BluetoothAdapter? by inject()
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()

    private val model: ChangeFrequencyViewModel by viewModel {
        parametersOf(intent.getParcelableExtra<UIDevice>(Contracts.ARG_DEVICE))
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
        binding = ActivityChangeFrequencyStationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        if (model.device.isEmpty()) {
            Timber.d("Could not start ChangeFrequencyActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        with(binding.toolbar) {
            setNavigationOnClickListener { finishActivity() }
            subtitle = model.device.name
        }

        with(binding.description) {
            movementMethod =
                me.saket.bettermovementmethod.BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, url ->
                        analytics.trackEventSelectContent(
                            Analytics.ParamValue.DOCUMENTATION_FREQUENCY.paramValue
                        )
                        navigator.openWebsite(context, url)
                        return@setOnLinkClickListener true
                    }
                }
            setHtml(R.string.set_frequency_desc, getString(R.string.helium_frequencies_mapping_url))
        }

        setListeners()

        model.onFrequencies().observe(this) { result ->
            if (result.country.isNullOrEmpty()) {
                binding.frequencySelectedText.visibility = View.GONE
            } else {
                binding.frequencySelectedText.text = getString(
                    R.string.changing_frequency_selected_text, result.country
                )
            }

            binding.frequenciesSelector.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_dropdown_item, result.frequencies
            )
        }

        model.onStatus().observe(this) {
            onNewStatus(it)
        }

        model.getCountryAndFrequencies()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.CHANGE_STATION_FREQUENCY,
            ChangeFrequencyActivity::class.simpleName,
            model.device.id
        )
    }

    private fun finishActivity() {
        model.scanningJob.cancel()
        finish()
    }

    private fun setListeners() {
        binding.confirmFrequencyToggle.setOnCheckedChangeListener { _, checked ->
            binding.changeFrequencyBtn.isEnabled = checked
        }

        binding.backButton.setOnClickListener {
            analytics.trackEventUserAction(
                actionName = Analytics.ParamValue.CHANGE_FREQUENCY_RESULT.paramValue,
                contentType = Analytics.ParamValue.CHANGE_FREQUENCY.paramValue,
                Pair(Analytics.CustomParam.ACTION.paramName, Analytics.ParamValue.CANCEL.paramValue)
            )
            finishActivity()
        }

        binding.changeFrequencyBtn.setOnClickListener {
            analytics.trackEventUserAction(
                actionName = Analytics.ParamValue.CHANGE_FREQUENCY_RESULT.paramValue,
                contentType = Analytics.ParamValue.CHANGE_FREQUENCY.paramValue,
                Pair(Analytics.CustomParam.ACTION.paramName, Analytics.ParamValue.CHANGE.paramValue)
            )
            model.setSelectedFrequency(binding.frequenciesSelector.selectedItemPosition)
            initBluetoothAndStart()
            binding.frequencySelectorContainer.hide(null)
            binding.bleActionFlow.setVisible(true)
        }

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
            model.scanConnectAndChangeFrequency()
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
                    contentName = Analytics.ParamValue.CHANGE_FREQUENCY_RESULT.paramValue,
                    contentId = Analytics.ParamValue.CHANGE_FREQUENCY_RESULT_ID.paramValue,
                    success = 1L
                )
            }
            Status.LOADING -> {
                onLoadingStatusUpdate(it.data?.status)
            }
            Status.ERROR -> {
                onErrorStatusUpdate(it)
                analytics.trackEventViewContent(
                    contentName = Analytics.ParamValue.CHANGE_FREQUENCY_RESULT.paramValue,
                    contentId = Analytics.ParamValue.CHANGE_FREQUENCY_RESULT_ID.paramValue,
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
                    sendSupportEmail(it)
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
                onGranted = { model.scanConnectAndChangeFrequency() },
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
                onGranted = { model.scanConnectAndChangeFrequency() },
                onDenied = {
                    binding.bleActionFlow.onError(true, R.string.no_bluetooth_access)
                })
        }
    }

    private fun sendSupportEmail(errorCode: String?) {
        navigator.sendSupportEmail(
            this,
            subject = getString(R.string.support_email_subject_helium_changing_frequency_failed),
            body = getString(
                R.string.support_email_body_helium_failed,
                model.device.name,
                errorCode ?: getString(R.string.unknown)
            )
        )
    }
}
