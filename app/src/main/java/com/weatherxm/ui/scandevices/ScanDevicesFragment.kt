package com.weatherxm.ui.scandevices

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import com.weatherxm.databinding.FragmentScanDevicesBinding
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.home.HomeViewModel
import kotlinx.coroutines.launch

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
                // TODO: What to do/show when user doesn't give access to bluetooth?
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

        binding.claimDeviceManually.setOnClickListener {
            homeViewModel.claimManually()
            dismiss()
        }

        model.onNewAdvertisement().observe(this) {
            adapter.submitList(it)
            adapter.notifyDataSetChanged()
        }

        context?.let {
            val bluetoothAdapter: BluetoothAdapter? =
                ContextCompat.getSystemService(it, BluetoothManager::class.java)?.adapter

            if (bluetoothAdapter == null) {
                // Device doesn't support Bluetooth
                // TODO: What to do/show when device doesn't support bluetooth?
            } else if (bluetoothAdapter.isEnabled) {
                checkAndScanBleDevices()
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
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
                rationaleTitle = getString(R.string.perm_location_bluetooth_title),
                rationaleMessage = getString(R.string.perm_location_bluetooth_desc),
                onGranted = {
                    lifecycleScope.launch {
                        model.scanBleDevices()
                    }
                }
            )
        } else {
            checkPermissionsAndThen(
                permissions = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                rationaleTitle = getString(R.string.perm_location_bluetooth_title),
                rationaleMessage = getString(R.string.perm_location_bluetooth_desc),
                onGranted = {
                    lifecycleScope.launch {
                        model.scanBleDevices()
                    }
                }
            )
        }
    }
}
