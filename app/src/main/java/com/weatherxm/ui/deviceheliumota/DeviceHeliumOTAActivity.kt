package com.weatherxm.ui.deviceheliumota

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityHeliumOtaBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.common.getParcelableExtra
import com.weatherxm.ui.common.toast
import com.weatherxm.util.applyInsets
import com.weatherxm.util.setHtml
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class DeviceHeliumOTAActivity : AppCompatActivity(), KoinComponent {

    companion object {
        const val ARG_DEVICE = "device"
    }

    private lateinit var binding: ActivityHeliumOtaBinding
    private val bluetoothAdapter: BluetoothAdapter? by inject()
    private val navigator: Navigator by inject()

    private val model: DeviceHeliumOTAViewModel by viewModel {
        parametersOf(getParcelableExtra(ARG_DEVICE, Device.empty()))
    }

    // TODO: Remove when backend is ready
    private val findZipFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            result.data?.data?.let {
                model.update(it)
            }
        }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                startScan()
            } else {
                setBluetoothStatusError(getString(R.string.bluetooth_not_enabled))
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

        binding.pairFirstStep.setHtml(R.string.reset_ble_first_step)
        binding.pairSecondStep.setHtml(R.string.tap_pair_device_button)

        binding.scanAgain.setOnClickListener {
            initBluetoothAndStart()
        }

        binding.pairDevice.setOnClickListener {
            model.pairDevice()
        }

        binding.cancel.setOnClickListener {
            finish()
        }

        binding.retry.setOnClickListener {
            model.setPeripheral()
        }

        binding.viewStation.setOnClickListener {
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(ARG_DEVICE, model.device)
            )
            finish()
        }

        model.onStatus().observe(this) {
            onNewStatus(it)
        }

        model.onInstallingProgress().observe(this) {
            binding.installationProgressBar.progress = it
        }

        // TODO: Remove when backend is ready
        model.onDownloadFile().observe(this) {
            if (it) {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/zip")

                findZipFileLauncher.launch(intent)
            }
        }

        initBluetoothAndStart()
    }

    private fun onNewStatus(it: Resource<State>) {
        when (it.status) {
            Status.SUCCESS -> {
                onSuccessStatusUpdate()
            }
            Status.LOADING -> {
                onLoadingStatusUpdate(it.data?.status)
            }
            Status.ERROR -> {
                onErrorStatusUpdate(it)
            }
        }
    }

    private fun onSuccessStatusUpdate() {
        hideButtons()
        binding.steps.visibility = View.GONE
        binding.status.clear()
        binding.status.animation(R.raw.anim_success)
        binding.status.title(R.string.station_updated)
        binding.status.subtitle(R.string.station_updated_subtitle)
        binding.viewStation.visibility = View.VISIBLE
    }

    private fun onErrorStatusUpdate(resource: Resource<State>) {
        hideButtons()
        binding.steps.visibility = View.GONE

        when (resource.data?.status) {
            OTAStatus.SCAN_FOR_STATION -> {
                binding.status.clear()
                binding.status.animation(R.raw.anim_error)
                if (resource.data.failure is BluetoothError.DeviceNotFound) {
                    binding.status.title(R.string.station_not_in_range)
                } else {
                    binding.status.title(R.string.scan_failed_title)
                }
                binding.status.subtitle(resource.message)
                binding.scanAgain.visibility = View.VISIBLE
            }
            OTAStatus.PAIR_STATION -> {
                binding.status.hide()
                binding.notPairedInfoContainer.visibility = View.VISIBLE
                binding.pairDevice.visibility = View.VISIBLE
            }
            OTAStatus.CONNECT_TO_STATION, OTAStatus.DOWNLOADING, OTAStatus.INSTALLING -> {
                binding.failureButtonsContainer.visibility = View.VISIBLE
                binding.status.clear()
                binding.status.animation(R.raw.anim_error)
                binding.status.title(R.string.update_failed)
                val errorIdentifier = if (resource.data.otaError != null) {
                    getString(
                        R.string.error_helium_ota_failed,
                        resource.data.otaError,
                        resource.data.otaErrorType,
                        resource.data.otaErrorMessage
                    )
                } else {
                    resource.message
                }
                binding.status.htmlSubtitle(R.string.update_failed_message, errorIdentifier) {
                    sendSupportEmail(resource.message)
                }
                binding.status.action(getString(R.string.title_contact_support))
                binding.status.listener { sendSupportEmail(errorIdentifier) }
            }
            else -> {
                toast(R.string.error_reach_out_short)
            }
        }
    }

    private fun onLoadingStatusUpdate(status: OTAStatus?) {
        if (!binding.steps.isVisible) {
            hideButtons()
            binding.notPairedInfoContainer.visibility = View.GONE
            binding.installationProgressBar.visibility = View.GONE
            binding.steps.visibility = View.VISIBLE
            binding.status.clear()
            binding.status.animation(R.raw.anim_loading)
            binding.status.show()
        }
        when (status) {
            OTAStatus.CONNECT_TO_STATION -> {
                binding.status.title(R.string.connecting_to_station)
                binding.firstStep.typeface = Typeface.DEFAULT_BOLD
            }
            OTAStatus.DOWNLOADING -> {
                binding.status.title(R.string.downloading_update)
                binding.status.htmlSubtitle(R.string.downloading_update_subtitle)
                binding.firstStep.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_checkmark, 0, 0, 0
                )
                binding.secondStep.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_two_filled, 0, 0, 0
                )
                binding.firstStep.typeface = Typeface.DEFAULT
                binding.secondStep.typeface = Typeface.DEFAULT_BOLD
            }
            else -> {
                binding.status.title(R.string.installing_update)
                binding.status.htmlSubtitle(R.string.installing_update_subtitle)
                binding.secondStep.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_checkmark, 0, 0, 0
                )
                binding.thirdStep.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_three_filled, 0, 0, 0
                )
                binding.secondStep.typeface = Typeface.DEFAULT
                binding.thirdStep.typeface = Typeface.DEFAULT_BOLD
                binding.installationProgressBar.visibility = View.VISIBLE
            }
        }
    }

    private fun hideButtons() {
        binding.viewStation.visibility = View.INVISIBLE
        binding.failureButtonsContainer.visibility = View.INVISIBLE
        binding.scanAgain.visibility = View.INVISIBLE
        binding.pairDevice.visibility = View.INVISIBLE
    }

    private fun sendSupportEmail(errorCode: String?) {
        navigator.sendSupportEmail(
            this,
            recipient = getString(R.string.support_email_recipient),
            subject = getString(R.string.support_email_subject_helium_ota_failed),
            body = getString(
                R.string.support_email_body_helium_ota_failed,
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
                    checkPermissionsAndThen(
                        permissions = arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        rationaleTitle = getString(R.string.permission_bluetooth_title),
                        rationaleMessage = getString(R.string.perm_bluetooth_scanning_desc),
                        onGranted = {
                            navigator.showBluetoothEnablePrompt(enableBluetoothLauncher)
                        },
                        onDenied = {
                            setBluetoothStatusError(getString(R.string.no_bluetooth_access))
                        })
                } else {
                    navigator.showBluetoothEnablePrompt(enableBluetoothLauncher)
                }
            }
        } ?: run {
            setBluetoothStatusError(getString(R.string.no_bluetooth_available))
        }
    }

    private fun startScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkPermissionsAndThen(
                permissions = arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                rationaleTitle = getString(R.string.permission_bluetooth_title),
                rationaleMessage = getString(R.string.perm_bluetooth_scanning_desc),
                onGranted = { model.startScan() },
                onDenied = { setBluetoothStatusError(getString(R.string.no_bluetooth_access)) }
            )
        } else {
            checkPermissionsAndThen(
                permissions = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                rationaleTitle = getString(R.string.permission_location_title),
                rationaleMessage = getString(R.string.perm_location_scanning_desc),
                onGranted = { model.startScan() },
                onDenied = { setBluetoothStatusError(getString(R.string.no_bluetooth_access)) }
            )
        }
    }

    private fun setBluetoothStatusError(title: String, subtitle: String? = null) {
        with(binding) {
            steps.visibility = View.GONE
            status.clear()
            status.animation(R.raw.anim_error)
            status.title(title)
            status.subtitle(subtitle)
            scanAgain.visibility = View.VISIBLE
        }
    }
}
