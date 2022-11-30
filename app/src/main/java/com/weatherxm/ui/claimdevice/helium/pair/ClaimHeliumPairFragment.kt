package com.weatherxm.ui.claimdevice.helium.pair

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentClaimHeliumPairBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.helium.verify.ClaimHeliumVerifyViewModel
import com.weatherxm.ui.common.ActionDialogFragment
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.util.setBluetoothDrawable
import com.weatherxm.util.setHtml
import com.weatherxm.util.setNoDevicesFoundDrawable
import com.weatherxm.util.setWarningDrawable
import org.koin.android.ext.android.inject
import timber.log.Timber

class ClaimHeliumPairFragment : Fragment() {
    private val model: ClaimHeliumPairViewModel by viewModels()
    private val parentModel: ClaimHeliumViewModel by activityViewModels()
    private val verifyModel: ClaimHeliumVerifyViewModel by activityViewModels()
    private val navigator: Navigator by inject()
    private val bluetoothAdapter: BluetoothAdapter? by inject()
    private lateinit var binding: FragmentClaimHeliumPairBinding
    private lateinit var adapter: ScannedDevicesListAdapter

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                checkAndScanBleDevices()
            } else {
                binding.infoIcon.setWarningDrawable(requireContext())
                showInfoMessage(R.string.bluetooth_not_enabled, R.string.bluetooth_not_enabled_desc)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimHeliumPairBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ScannedDevicesListAdapter {
            model.setupBluetoothClaiming(it.address)
            binding.progressBar.visibility = GONE
        }

        binding.recycler.adapter = adapter

        binding.scanAgain.setOnClickListener {
            adapter.submitList(mutableListOf())
            checkAndScanBleDevices()
        }

        binding.accessBluetoothPrompt.setOnClickListener {
            Timber.d("Going to application settings")
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", context?.packageName, null)
            startActivity(intent)
        }

        model.onNewScannedDevice().observe(viewLifecycleOwner) {
            adapter.submitList(it)
            adapter.notifyDataSetChanged()
            binding.infoContainer.visibility = GONE
            binding.recycler.visibility = VISIBLE
        }

        model.onScanStatus().observe(viewLifecycleOwner) {
            updateUI(it)
        }

        model.onScanProgress().observe(viewLifecycleOwner) {
            binding.progressBar.progress = it
        }

        model.onBLEError().observe(viewLifecycleOwner) {
            showErrorDialog(it)
        }

        model.onBLEDevEUI().observe(viewLifecycleOwner) {
            verifyModel.setDeviceEUI(it)
        }

        model.onBLEClaimingKey().observe(viewLifecycleOwner) {
            verifyModel.setDeviceKey(it)
            parentModel.next()
        }

        bluetoothAdapter?.let {
            if (it.isEnabled) {
                checkAndScanBleDevices()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    checkPermissionsAndThen(
                        permissions = arrayOf(BLUETOOTH_CONNECT),
                        rationaleTitle = getString(R.string.permission_bluetooth_title),
                        rationaleMessage = getString(R.string.perm_bluetooth_scanning_desc),
                        onGranted = {
                            navigator.showBluetoothEnablePrompt(enableBluetoothLauncher)
                        },
                        onDenied = { showNoBluetoothAccessText() })
                } else {
                    navigator.showBluetoothEnablePrompt(enableBluetoothLauncher)
                }
            }
        } ?: run {
            binding.infoIcon.setBluetoothDrawable(requireContext())
            showInfoMessage(R.string.no_bluetooth_available, R.string.no_bluetooth_available_desc)
        }
    }

    private fun showErrorDialog(uiError: UIError) {
        ActionDialogFragment
            .Builder(
                title = getString(R.string.pairing_failed),
                message = uiError.errorMessage
            )
            .onNegativeClick(getString(R.string.action_quit_claiming)) {
                parentModel.cancel()
            }
            .onPositiveClick(getString(R.string.action_try_again)) {
                uiError.retryFunction?.invoke()
            }
            .build()
            .show(this)
    }

    private fun updateUI(result: Resource<Unit>) {
        when (result.status) {
            Status.SUCCESS -> {
                binding.progressBar.visibility = GONE
                binding.scanAgain.isEnabled = true
                if (adapter.currentList.isNotEmpty()) {
                    binding.infoContainer.visibility = GONE
                } else {
                    binding.recycler.visibility = GONE
                    binding.infoIcon.setNoDevicesFoundDrawable(requireContext())
                    showInfoMessage(R.string.no_devices_found, R.string.no_devices_found_desc)
                }
            }
            Status.ERROR -> {
                binding.progressBar.visibility = GONE
                binding.scanAgain.isEnabled = true
                binding.recycler.visibility = GONE
                binding.infoIcon.setWarningDrawable(requireContext())
                showInfoMessage(R.string.scan_failed_title, R.string.scan_failed_desc)
            }
            Status.LOADING -> {
                binding.progressBar.visibility = VISIBLE
                binding.scanAgain.isEnabled = false
                binding.recycler.visibility = GONE
                binding.infoIcon.setBluetoothDrawable(requireContext())
                showInfoMessage(R.string.scanning_in_progress, null)
            }
        }
    }

    private fun showInfoMessage(@StringRes title: Int, @StringRes subtitle: Int?) {
        binding.infoTitle.text = getString(title)
        with(binding.infoSubtitle) {
            subtitle?.let {
                setHtml(it)
                visibility = VISIBLE
            } ?: run {
                visibility = GONE
            }
        }
        binding.infoContainer.visibility = VISIBLE
    }

    private fun showNoBluetoothAccessText() {
        binding.infoIcon.setBluetoothDrawable(requireContext())
        showInfoMessage(R.string.no_bluetooth_access, R.string.no_bluetooth_access_desc)
        binding.accessBluetoothPrompt.visibility = VISIBLE
    }

    private fun checkAndScanBleDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkPermissionsAndThen(
                permissions = arrayOf(BLUETOOTH_SCAN, BLUETOOTH_CONNECT),
                rationaleTitle = getString(R.string.permission_bluetooth_title),
                rationaleMessage = getString(R.string.perm_bluetooth_scanning_desc),
                onGranted = { model.scanBleDevices() },
                onDenied = { showNoBluetoothAccessText() }
            )
        } else {
            checkPermissionsAndThen(
                permissions = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                rationaleTitle = getString(R.string.permission_location_title),
                rationaleMessage = getString(R.string.perm_location_scanning_desc),
                onGranted = { model.scanBleDevices() },
                onDenied = {
                    binding.infoIcon.setBluetoothDrawable(requireContext())
                    showInfoMessage(R.string.no_location_access, R.string.no_location_access_desc)
                    binding.accessBluetoothPrompt.visibility = VISIBLE
                }
            )
        }
    }
}
