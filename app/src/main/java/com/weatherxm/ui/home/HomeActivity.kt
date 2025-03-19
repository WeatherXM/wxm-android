package com.weatherxm.ui.home

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.mapbox.geojson.Point
import com.weatherxm.R
import com.weatherxm.data.models.Location
import com.weatherxm.databinding.ActivityHomeBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.BaseMapFragment
import com.weatherxm.ui.components.compose.TermsDialog
import com.weatherxm.ui.explorer.ExplorerViewModel
import com.weatherxm.ui.home.devices.DevicesViewModel
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class HomeActivity : BaseActivity(), BaseMapFragment.OnMapDebugInfoListener {
    private val model: HomeViewModel by viewModel()
    private val explorerModel: ExplorerViewModel by viewModel()
    private val devicesViewModel: DevicesViewModel by viewModel()

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController

    init {
        lifecycleScope.launch {
            withCreated {
                requestNotificationsPermissions()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment)

        // Setup navigation view
        binding.navView.setupWithNavController(navController)

        explorerModel.onStatus().observe(this) { resource ->
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

        model.showOverlayViews().observe(this) {
            onScroll(it)
        }

        devicesViewModel.devices().observe(this) {
            onDevices(it)
        }

        /**
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
            model.setClaimingBadgeShouldShow(false)
            navigator.showClaimSelectStationType(this)
        }

        binding.networkStatsBtn.setOnClickListener {
            navigator.showNetworkStats(this)
        }

        binding.buyStationBtn.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.shop_url))
        }

        binding.followStationExplorerBtn.setOnClickListener {
            NavigationUI.onNavDestinationSelected(
                binding.navView.menu.findItem(R.id.navigation_explorer), navController
            )
        }

        model.onWalletWarnings().observe(this) {
            handleBadge(it.showMissingBadge)
        }

        // Disable BottomNavigationView bottom padding, added by default, and add margin
        // https://github.com/material-components/material-components-android/commit/276bec8385ec877548fc84994c0a016de2428567
        binding.navView.applyInsetter {
            type(navigationBars = true) {
                padding(horizontal = false, vertical = false)
                margin(bottom = true)
            }
        }

        with(intent.parcelable<Location>(Contracts.ARG_CELL_CENTER)) {
            this?.let {
                NavigationUI.onNavDestinationSelected(
                    binding.navView.menu.findItem(R.id.navigation_explorer), navController
                )
                explorerModel.navigateToLocation(it)
            }
        }

        binding.dialogComposeView.setContent {
            TermsDialog(model.shouldShowTerms.value) {
                model.setAcceptTerms()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Fetch user's devices
        devicesViewModel.fetch()

        /**
         * Changing the theme from Profile -> Settings and going back to profile
         * shows the "Add Device" floating button visible again. This code is to fix this.
         */
        val navDestination = navController.currentDestination?.id
        binding.networkStatsBtn.visible(navDestination == R.id.navigation_explorer)
        binding.myLocationBtn.visible(navDestination == R.id.navigation_explorer)
        /**
         * Don't use the visible function for the addButton
         * because of a specific case hiding it in the devices list.
         * Therefore we use the hide() function which fits our purpose.
         */
        if (navDestination == R.id.navigation_profile) {
            binding.addDevice.hide()
        }
    }

    private fun onScroll(shouldShowOverlayItems: Boolean) {
        if (shouldShowOverlayItems) {
            if (navController.currentDestination?.id == R.id.navigation_devices) {
                binding.addDevice.show()
            }
            binding.navView.show()
        } else {
            binding.addDevice.hide()
            binding.navView.hide()
        }
    }

    private fun onDevices(resource: Resource<List<UIDevice>>) {
        val currentDestination = navController.currentDestination?.id
        if (resource.status == Status.SUCCESS && currentDestination == R.id.navigation_devices) {
            model.getRemoteBanners()
            checkForNoDevices()
        } else {
            binding.emptyContainer.visible(false)
            binding.claimRedDot.visible(false)
        }
    }

    private fun checkForNoDevices() {
        if (devicesViewModel.hasNoDevices()) {
            binding.emptyContainer.visible(true)
        }
        if (model.getClaimingBadgeShouldShow()) {
            binding.claimRedDot.visible(true)
        }
    }

    private fun onNavigationChanged(destination: NavDestination) {
        if (snackbar?.isShown == true) snackbar?.dismiss()
        when (destination.id) {
            R.id.navigation_devices -> {
                checkForNoDevices()
                binding.addDevice.show()
            }
            R.id.navigation_explorer -> {
                explorerModel.setExplorerAfterLoggedIn(true)
                binding.emptyContainer.visible(false)
                binding.claimRedDot.visible(false)
                binding.addDevice.hide()
            }
            else -> {
                binding.emptyContainer.visible(false)
                binding.claimRedDot.visible(false)
                binding.addDevice.hide()
            }
        }
        binding.navView.show()
        binding.networkStatsBtn.visible(destination.id == R.id.navigation_explorer)
        binding.myLocationBtn.visible(destination.id == R.id.navigation_explorer)
    }

    private fun onExplorerState(resource: Resource<Unit>) {
        Timber.d("Status updated: ${resource.status}")
        when (resource.status) {
            Status.SUCCESS -> {
                snackbar?.dismiss()
            }
            Status.ERROR -> {
                Timber.d("Got error: $resource.message")
                resource.message?.let {
                    showSnackbarMessage(binding.root, it, callback = { explorerModel.fetch() })
                }
            }
            Status.LOADING -> {
                snackbar?.dismiss()
            }
        }
    }

    private fun handleBadge(missingWallet: Boolean) {
        if (missingWallet && model.hasDevices() == true) {
            binding.navView.getOrCreateBadge(R.id.navigation_profile)
        } else {
            binding.navView.removeBadge(R.id.navigation_profile)
        }
    }

    override fun onMapDebugInfoUpdated(zoom: Double, center: Point) {
        // Do nothing
    }

    fun navView() = binding.navView
}
