package com.weatherxm.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.BOND_NONE
import android.bluetooth.BluetoothDevice.EXTRA_BOND_STATE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.data.bluetooth.BluetoothConnectionManager
import com.weatherxm.databinding.ActivityHomeBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.claimdevice.ClaimDeviceActivity
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.explorer.ExplorerViewModel
import com.weatherxm.ui.home.devices.DevicesViewModel
import com.weatherxm.ui.home.profile.ProfileViewModel
import com.weatherxm.util.hideIfNot
import com.weatherxm.util.showIfNot
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber


class HomeActivity : AppCompatActivity(), KoinComponent {
    private val navigator: Navigator by inject()
    private lateinit var binding: ActivityHomeBinding
    private val explorerModel: ExplorerViewModel by viewModels()
    private val devicesViewModel: DevicesViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    private var snackbar: Snackbar? = null

    // Register the launcher for the claim device activity and wait for a possible result
    private val claimDeviceLauncher =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                devicesViewModel.fetch()
            }
        }

    private val findZipFileLauncher =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            result.data?.data?.let {
                homeViewModel.update(it)
            }
        }

    /*
    * These 3 intentFilters and broadcastReceivers below are being used here for testing
    * purposes in order to debug and fix the bypassing of PIN in a correct way.
     */
    private val intentFilter = IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST)
    private val intentFilter2 = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
    private val intentFilter3 = IntentFilter()

    private val broadCastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_PAIRING_REQUEST == action) {
                val bluetoothDevice =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                bluetoothDevice?.let {
                    // TODO: Check on different android versions 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        checkPermissionsAndThen(
                            permissions = arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                            rationaleTitle = getString(R.string.perm_location_bluetooth_title),
                            rationaleMessage = getString(R.string.perm_location_bluetooth_desc),
                            onGranted = {
                                Timber.d("Auto entering BLE PIN of the device")
                                it.setPin(BluetoothConnectionManager.DEFAULT_PAIR_PIN.toByteArray())
                                it.createBond()
                                abortBroadcast()
                            }
                        )
                    } else {
                        checkPermissionsAndThen(
                            permissions = arrayOf(Manifest.permission.BLUETOOTH_ADMIN),
                            rationaleTitle = getString(R.string.perm_location_bluetooth_title),
                            rationaleMessage = getString(R.string.perm_location_bluetooth_desc),
                            onGranted = {
                                Timber.d("Auto entering BLE PIN of the device")
                                it.setPin(BluetoothConnectionManager.DEFAULT_PAIR_PIN.toByteArray())
                                it.createBond()
                                abortBroadcast()
                            }
                        )
                    }
                }
            }
        }
    }

    private val broadCastReceiver2: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val bluetoothDevice =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val currentBondState = bluetoothDevice?.bondState
                val bondStateFromIntent = intent.getIntExtra(EXTRA_BOND_STATE, BOND_NONE)

                when (currentBondState) {
                    BluetoothDevice.BOND_BONDED -> {
                        println("BOND_BONDED")
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        println("BOND_BONDING")
                    }
                    BluetoothDevice.BOND_NONE -> {
                        println("BOND_NONE")
                    }
                }
                println("currentBondState: $currentBondState")
                println("bondState from intent: $bondStateFromIntent")
                println("BOND_BONDED: ${BluetoothDevice.BOND_BONDED}")
                println("BOND_BONDING: ${BluetoothDevice.BOND_BONDING}")
                println("BOND_NONE: ${BluetoothDevice.BOND_NONE}")
            }
        }
    }

    private val broadCastReceiver3: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    println("ACTION_BOND_STATE_CHANGED")
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    println("ACTION_ACL_DISCONNECTED")
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    println("ACTION_BOND_STATE_CHANGED")
                }
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController = findNavController(R.id.nav_host_fragment)

        // Setup navigation view
        binding.navView.setupWithNavController(navController)

        explorerModel.onHexSelected().observe(this) {
            navigator.showPublicDevicesList(supportFragmentManager)
        }

        explorerModel.onPublicDeviceSelected().observe(this) {
            navigator.showDeviceDetails(supportFragmentManager, it)
        }

        explorerModel.explorerState().observe(this) { resource ->
            Timber.d("Status updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    if (navController.currentDestination?.id == R.id.navigation_explorer) {
                        binding.devicesCount.text =
                            getString(R.string.devices_count, resource.data?.totalDevices)
                        binding.devicesCountCard.visibility = View.VISIBLE
                    }
                    snackbar?.dismiss()
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    resource.message?.let { showErrorOnMapLoading(it) }
                }
                Status.LOADING -> {
                    snackbar?.dismiss()
                }
            }
        }

        devicesViewModel.showOverlayViews().observe(this) { shouldShow ->
            if (shouldShow) {
                binding.addDevice.showIfNot()
                binding.navView.showIfNot()
            } else if (!shouldShow) {
                binding.addDevice.hideIfNot()
                binding.navView.hideIfNot()
            }
        }

        /*
         * Show/hide FAB and devices count label
         * based on selected navigation item and dismiss snackbar if shown
         */
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (snackbar?.isShown == true) {
                snackbar?.dismiss()
            }
            when (destination.id) {
                R.id.navigation_devices -> {
                    binding.addDevice.showIfNot()
                    binding.devicesCountCard.visibility = View.GONE
                }
                else -> {
                    binding.addDevice.hideIfNot()
                    binding.devicesCountCard.visibility = View.GONE
                }
            }
        }

        binding.addDevice.setOnClickListener {
            navigator.showScanDialog(supportFragmentManager)
        }

        profileViewModel.wallet().observe(this) {
            handleBadge(!it.isNullOrEmpty())
        }

        homeViewModel.onClaimManually().observe(this) {
            if (it) {
                claimDeviceLauncher.launch(Intent(this, ClaimDeviceActivity::class.java))
            }
        }

        homeViewModel.onScannedDeviceSelected().observe(this) {
            GlobalScope.launch {
                intentFilter3.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                intentFilter3.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                intentFilter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
                intentFilter2.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
                intentFilter3.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
                registerReceiver(broadCastReceiver, intentFilter)
                registerReceiver(broadCastReceiver2, intentFilter2)
                registerReceiver(broadCastReceiver3, intentFilter3)

                homeViewModel.setPeripheral(it.address)
                homeViewModel.connectToPeripheral()
            }
        }

        homeViewModel.onConnectedDevice().observe(this) {
//            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
//                .setType("application/zip")
//
//            findZipFileLauncher.launch(intent)
        }

        // Disable BottomNavigationView bottom padding, added by default, and add margin
        // https://github.com/material-components/material-components-android/commit/276bec8385ec877548fc84994c0a016de2428567
        binding.navView.applyInsetter {
            type(navigationBars = true) {
                padding(horizontal = false, vertical = false)
                margin(bottom = true)
            }
        }

        binding.devicesCountCard.applyInsetter {
            type(statusBars = true) {
                margin(left = false, top = true, right = false, bottom = false)
            }
        }

        // Fetch user's devices
        devicesViewModel.fetch()
    }

    private fun handleBadge(hasWallet: Boolean) {
        if (!hasWallet) {
            binding.navView.getOrCreateBadge(R.id.navigation_profile)
        } else {
            binding.navView.removeBadge(R.id.navigation_profile)
        }
    }

    private fun showErrorOnMapLoading(message: String) {
        if (snackbar?.isShown == true) {
            snackbar?.dismiss()
        }
        snackbar = Snackbar
            .make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.action_retry) {
                explorerModel.fetch()
            }
        snackbar?.show()
    }
}
