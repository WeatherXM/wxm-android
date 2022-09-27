package com.weatherxm.ui.claimdevice.scandevices

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentScanDevicesBinding
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.home.HomeViewModel
import com.weatherxm.util.setHtml
import kotlinx.coroutines.launch
import timber.log.Timber

class ScanDevicesFragment : BottomSheetDialogFragment() {
    private val model: ScanDevicesViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var binding: FragmentScanDevicesBinding
    private lateinit var adapter: ScannedDevicesListAdapter

    companion object {
        const val TAG = "ScanDevicesFragment"
    }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                checkAndScanBleDevices()
            } else {
                binding.infoTitle.text = getString(R.string.bluetooth_not_enabled)
                binding.infoSubtitle.setHtml(R.string.bluetooth_not_enabled_desc)
                binding.infoContainer.visibility = View.VISIBLE
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentScanDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ScannedDevicesListAdapter {
            homeViewModel.selectScannedDevice(it)
            dismiss()
        }

        binding.recycler.adapter = adapter

        binding.closePopup.setOnClickListener {
            dismiss()
        }

        binding.scanAgain.setOnClickListener {
            if (!model.isScanningRunning()) {
                adapter.submitList(mutableListOf())
                checkAndScanBleDevices()
            }
        }

        binding.accessBluetoothPrompt.setOnClickListener {
            Timber.d("Going to application settings")
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", context?.packageName, null)
            startActivity(intent)
        }

        binding.claimDeviceManually.setOnClickListener {
            homeViewModel.claimManually()
            dismiss()
        }

        model.onNewAdvertisement().observe(this) {
            adapter.submitList(it)
            adapter.notifyDataSetChanged()
            binding.infoContainer.visibility = View.GONE
            binding.recycler.visibility = View.VISIBLE
        }

        model.onProgress().observe(this) {
            updateUI(it)
        }

        context?.let {
            val bluetoothAdapter: BluetoothAdapter? =
                ContextCompat.getSystemService(it, BluetoothManager::class.java)?.adapter

            if (bluetoothAdapter == null) {
                binding.infoTitle.text = getString(R.string.no_bluetooth_available)
                binding.infoSubtitle.setHtml(R.string.no_bluetooth_available_desc)
                binding.infoContainer.visibility = View.VISIBLE
            } else if (bluetoothAdapter.isEnabled) {
                checkAndScanBleDevices()
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    checkPermissionsAndThen(
                        permissions = arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT
                        ),
                        rationaleTitle = getString(R.string.permission_bluetooth_title),
                        rationaleMessage = getString(R.string.perm_bluetooth_scanning_desc),
                        onGranted = {
                            lifecycleScope.launch {
                                enableBluetoothLauncher.launch(enableBtIntent)
                            }
                        },
                        onDenied = {
                            binding.infoTitle.text = getString(R.string.no_bluetooth_access)
                            binding.infoSubtitle.setHtml(R.string.no_bluetooth_access_desc)
                            binding.scanAgain.visibility = View.GONE
                            binding.accessBluetoothPrompt.visibility = View.VISIBLE
                            binding.infoContainer.visibility = View.VISIBLE
                        }
                    )
                } else {
                    enableBluetoothLauncher.launch(enableBtIntent)
                }
            }
        }
    }

    private fun updateUI(result: Resource<Unit>) {
        when (result.status) {
            Status.SUCCESS -> {
                if (adapter.currentList.isNotEmpty()) {
                    binding.infoContainer.visibility = View.GONE
                } else {
                    binding.infoTitle.text = getString(R.string.no_devices_found)
                    binding.infoSubtitle.text = getString(R.string.no_devices_found_desc)
                    binding.infoSubtitle.visibility = View.VISIBLE
                    binding.recycler.visibility = View.VISIBLE
                }
            }
            Status.ERROR -> {
                binding.infoTitle.text = getString(R.string.scan_failed_title)
                binding.infoSubtitle.setHtml(R.string.scan_failed_desc)
                binding.infoSubtitle.visibility = View.VISIBLE
                binding.recycler.visibility = View.VISIBLE
            }
            Status.LOADING -> {
                binding.recycler.visibility = View.GONE
                binding.infoTitle.text = getString(R.string.scanning_in_progress)
                binding.infoSubtitle.visibility = View.GONE
                binding.infoContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun checkAndScanBleDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkPermissionsAndThen(
                permissions = arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                rationaleTitle = getString(R.string.permission_bluetooth_title),
                rationaleMessage = getString(R.string.perm_bluetooth_scanning_desc),
                onGranted = {
                    lifecycleScope.launch {
                        model.scanBleDevices()
                    }
                },
                onDenied = {
                    binding.infoTitle.text = getString(R.string.no_bluetooth_access)
                    binding.infoSubtitle.setHtml(R.string.no_bluetooth_access_desc)
                    binding.scanAgain.visibility = View.GONE
                    binding.accessBluetoothPrompt.visibility = View.VISIBLE
                    binding.infoContainer.visibility = View.VISIBLE
                }
            )
        } else {
            checkPermissionsAndThen(
                permissions = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                rationaleTitle = getString(R.string.permission_location_title),
                rationaleMessage = getString(R.string.perm_location_scanning_desc),
                onGranted = {
                    lifecycleScope.launch {
                        model.scanBleDevices()
                    }
                },
                onDenied = {
                    binding.infoTitle.text = getString(R.string.no_location_access)
                    binding.infoSubtitle.setHtml(R.string.no_location_access_desc)
                    binding.scanAgain.visibility = View.GONE
                    binding.accessBluetoothPrompt.visibility = View.VISIBLE
                    binding.infoContainer.visibility = View.VISIBLE
                }
            )
        }
    }
}
