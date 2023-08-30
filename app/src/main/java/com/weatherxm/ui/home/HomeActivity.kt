package com.weatherxm.ui.home

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Point
import com.weatherxm.R
import com.weatherxm.data.Location
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityHomeBinding
import com.weatherxm.ui.BaseMapFragment
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.claimdevice.selectdevicetype.SelectDeviceTypeDialogFragment
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.explorer.ExplorerData
import com.weatherxm.ui.explorer.ExplorerViewModel
import com.weatherxm.ui.home.devices.DevicesViewModel
import dev.chrisbanes.insetter.applyInsetter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class HomeActivity : AppCompatActivity(), KoinComponent,
    BaseMapFragment.OnMapDebugInfoListener {
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

        explorerModel.onCellSelected().observe(this) {
            navigator.showCellInfo(this, it)
        }

        explorerModel.explorerState().observe(this) { resource ->
            onExplorerState(resource)
        }

        explorerModel.onSearchOpenStatus().observe(this) { isOpened ->
            if (isOpened) {
                binding.navView.hide()
                binding.networkStatsBtn.hide()
                binding.myLocationBtn.hide()
            } else {
                binding.navView.show()
                binding.networkStatsBtn.show()
                binding.myLocationBtn.show()
            }
        }

        devicesViewModel.showOverlayViews().observe(this) { shouldShow ->
            if (shouldShow) {
                binding.addDevice.show()
                binding.navView.show()
            } else if (!shouldShow) {
                binding.addDevice.hide()
                binding.navView.hide()
            }
        }

        /*
         * Show/hide FAB and devices count label
         * based on selected navigation item and dismiss snackbar if shown
         */
        navController.addOnDestinationChangedListener { _, destination, _ ->
            onNavigationChanged(destination)
        }

        binding.myLocationBtn.setOnClickListener {
            explorerModel.onMyLocation()
        }

        binding.addDevice.setOnClickListener {
            // Show device type selection dialog
            SelectDeviceTypeDialogFragment.newInstance { selectedDeviceType ->
                if (selectedDeviceType == DeviceType.HELIUM) {
                    navigator.showClaimHeliumFlow(this)
                } else {
                    navigator.showClaimM5Flow(this)
                }
            }.show(this)
        }

        binding.networkStatsBtn.setOnClickListener {
            navigator.showNetworkStats(this)
        }

        model.onWalletMissing().observe(this) {
            handleBadge(it)
        }

        model.onOpenExplorer().observe(this) {
            if (it == true) {
                NavigationUI.onNavDestinationSelected(
                    binding.navView.menu.findItem(R.id.navigation_explorer), navController
                )
            }
        }

        // Disable BottomNavigationView bottom padding, added by default, and add margin
        // https://github.com/material-components/material-components-android/commit/276bec8385ec877548fc84994c0a016de2428567
        binding.navView.applyInsetter {
            type(navigationBars = true) {
                padding(horizontal = false, vertical = false)
                margin(bottom = true)
            }
        }

        with(intent.getParcelableExtra<Location>(Contracts.ARG_CELL_CENTER)) {
            this?.let {
                navController.navigate(R.id.navigation_explorer)
                explorerModel.navigateToLocation(it)
            }
        }
    }

    private fun onNavigationChanged(destination: NavDestination) {
        if (snackbar?.isShown == true) snackbar?.dismiss()
        when (destination.id) {
            R.id.navigation_devices -> binding.addDevice.show()
            R.id.navigation_explorer -> {
                explorerModel.setExplorerAfterLoggedIn(true)
                binding.addDevice.hide()
            }
            else -> binding.addDevice.hide()
        }
        binding.navView.show()
        binding.networkStatsBtn.setVisible(destination.id == R.id.navigation_explorer)
        binding.myLocationBtn.setVisible(destination.id == R.id.navigation_explorer)
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
                binding.addDevice.show()
                binding.navView.show()
                binding.networkStatsBtn.setVisible(false)
                binding.myLocationBtn.setVisible(false)
            }
            R.id.navigation_explorer -> {
                binding.addDevice.hide()
                binding.networkStatsBtn.setVisible(true)
                binding.myLocationBtn.setVisible(true)
            }
            else -> {
                binding.addDevice.hide()
                binding.networkStatsBtn.setVisible(false)
                binding.myLocationBtn.setVisible(false)
            }
        }
    }

    private fun onExplorerState(resource: Resource<ExplorerData>) {
        Timber.d("Status updated: ${resource.status}")
        when (resource.status) {
            Status.SUCCESS -> {
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

    override fun onMapDebugInfoUpdated(zoom: Double, center: Point) {
        // Do nothing
    }
}
