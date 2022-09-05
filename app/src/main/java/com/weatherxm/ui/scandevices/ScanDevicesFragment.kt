package com.weatherxm.ui.scandevices

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun getTheme(): Int {
        return R.style.ThemeOverlay_WeatherXM_BottomSheetDialog
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

        checkAndScanBleDevices()
    }

    fun checkAndScanBleDevices() {
        // TODO: Confirm if all these permissions are needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkPermissionsAndThen(
                permissions = arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
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
        } else {
            checkPermissionsAndThen(
                permissions = arrayOf(
                    Manifest.permission.BLUETOOTH_ADMIN,
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
