package com.weatherxm.ui.home

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import arrow.core.Either
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.data.ApiError.UserError.ClaimError.ClaimCancelledError
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityHomeBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.claimdevice.selectdevicetype.SelectDeviceTypeDialogFragment
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.explorer.ExplorerViewModel
import com.weatherxm.ui.home.devices.DevicesViewModel
import com.weatherxm.util.hideIfNot
import com.weatherxm.util.showIfNot
import dev.chrisbanes.insetter.applyInsetter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class HomeActivity : AppCompatActivity(), KoinComponent {
    private val navigator: Navigator by inject()
    private val model: HomeViewModel by viewModels()
    private val explorerModel: ExplorerViewModel by viewModels()
    private val devicesViewModel: DevicesViewModel by viewModels()

    private var snackbar: Snackbar? = null

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment)

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
            if (snackbar?.isShown == true) snackbar?.dismiss()
            when (destination.id) {
                R.id.navigation_devices -> binding.addDevice.showIfNot()
                else -> binding.addDevice.hideIfNot()
            }
            binding.devicesCountCard.visibility = View.GONE
        }

        binding.addDevice.setOnClickListener {
            // Show device type selection dialog
            SelectDeviceTypeDialogFragment.newInstance { selectedDeviceType ->
                if (selectedDeviceType == DeviceType.HELIUM) {
                    navigator.showClaimHeliumFlow(this) { result ->
                        handleClaimResult(result)
                    }
                } else {
                    navigator.showClaimM5Flow(this) { result ->
                        handleClaimResult(result)
                    }
                }
            }.show(this)
        }

        model.onWalletMissing().observe(this) {
            handleBadge(it)
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
    }

    private fun handleClaimResult(result: Either<Failure, Device>) {
        Timber.d("Claim result: $result")
        result
            .tap {
                // Just log
                Timber.d("Device ${it.name} claimed successfully.")
            }
            .tapLeft {
                // Error is handled elsewhere
                if (it is ClaimCancelledError) {
                    toast(R.string.warn_cancelled)
                }
            }
    }

    override fun onResume() {
        super.onResume()

        // Fetch user's devices
        devicesViewModel.fetch()

        /*
        * Changing the theme from Profile -> Settings and going back to profile
        * shows the "Add Device" floating button visible again. This code is to fix this.
         */
        when (navController.currentDestination?.id) {
            R.id.navigation_devices -> {
                binding.addDevice.showIfNot()
                binding.devicesCountCard.visibility = View.GONE
            }
            R.id.navigation_explorer -> {
                binding.addDevice.hideIfNot()
                binding.devicesCountCard.visibility = View.VISIBLE
            }
            else -> {
                binding.addDevice.hideIfNot()
                binding.devicesCountCard.visibility = View.GONE
            }
        }
    }

    private fun handleBadge(missingWallet: Boolean) {
        if (missingWallet) {
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
