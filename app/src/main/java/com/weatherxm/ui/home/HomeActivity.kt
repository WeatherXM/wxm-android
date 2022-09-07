package com.weatherxm.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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
import com.weatherxm.databinding.ActivityHomeBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.claimdevice.ClaimDeviceActivity
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

    // TODO: This will be used in the Update activity where the flow is TBD. 
    private val findZipFileLauncher =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            result.data?.data?.let {
                homeViewModel.update(it)
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
                homeViewModel.setPeripheral(it.address)
                homeViewModel.connectToPeripheral()
            }
        }

        // TODO: For testing purposes. 
        homeViewModel.onConnectedDevice().observe(this) {
            // TODO: Check all different SDK versions in order to put the correct intent action
//            val intent = Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE)
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
