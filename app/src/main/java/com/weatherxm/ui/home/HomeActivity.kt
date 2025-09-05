package com.weatherxm.ui.home

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.get
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.mapbox.geojson.Point
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.models.Location
import com.weatherxm.databinding.ActivityHomeBinding
import com.weatherxm.service.workers.DevicesNotificationsWorker
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.BaseMapFragment
import com.weatherxm.ui.components.BaseMapFragment.Companion.ZOOMED_IN_ZOOM_LEVEL
import com.weatherxm.ui.components.compose.TermsDialog
import com.weatherxm.ui.home.devices.DevicesViewModel
import com.weatherxm.ui.home.explorer.ExplorerViewModel
import com.weatherxm.ui.home.explorer.MapLayerPickerDialogFragment
import com.weatherxm.ui.home.locations.LocationsViewModel
import com.weatherxm.ui.home.profile.ProfileViewModel
import com.weatherxm.util.AndroidBuildInfo
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class HomeActivity : BaseActivity(), BaseMapFragment.OnMapDebugInfoListener {
    private val model: HomeViewModel by viewModel()
    private val explorerModel: ExplorerViewModel by viewModel()
    private val devicesViewModel: DevicesViewModel by viewModel()
    private val profileViewModel: ProfileViewModel by viewModel()
    private val locationsViewModel: LocationsViewModel by viewModel()

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

        if (!AndroidBuildInfo.isSolana()) {
            navController.graph.remove(navController.graph[R.id.navigation_quests])
            binding.navView.menu.findItem(R.id.navigation_quests).isVisible = false
        }

        explorerModel.onStatus().observe(this) { resource ->
            onExplorerState(resource)
        }

        explorerModel.onSearchOpenStatus().observe(this) {
            onExplorerSearchOpenStatus(it)
        }

        locationsViewModel.onSearchOpenStatus().observe(this) {
            onLocationSearchOpenStatus(it)
        }

        model.onNavigateToQuests().observe(this) {
            binding.navView.menu.findItem(R.id.navigation_quests)?.let {
                NavigationUI.onNavDestinationSelected(it, navController)
            }
        }

        model.showOverlayViews().observe(this) {
            onScroll(it)
        }

        devicesViewModel.devices().observe(this) {
            onDevices(it)
        }

        setupAuthActions()

        /**
         * Show/hide FAB and devices count label
         * based on selected navigation item and dismiss snackbar if shown
         */
        navController.addOnDestinationChangedListener { _, destination, _ ->
            onNavigationChanged(destination)
        }

        binding.mapLayerPickerBtn.setOnClickListener {
            MapLayerPickerDialogFragment().show(this)
        }

        binding.myLocationBtn.setOnClickListener {
            onMyLocation()
        }

        binding.searchLocationBtn.setOnClickListener {
            locationsViewModel.searchBtnClicked()
        }

        binding.addDevice.setOnClickListener {
            model.setClaimingBadgeShouldShow(false)
            navigator.showClaimSelectStationType(this)
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

        with(intent.parcelable<Location>(Contracts.ARG_CELL_CENTER)) {
            this?.let {
                NavigationUI.onNavDestinationSelected(
                    binding.navView.menu.findItem(R.id.navigation_explorer), navController
                )
                explorerModel.navigateToLocation(it, ZOOMED_IN_ZOOM_LEVEL)
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

        // Check if we are logged in or not and set the respective variable to be used in tabs
        model.checkIfIsLoggedIn()

        // Fetch user's profile
        profileViewModel.fetchUser(model.isLoggedIn())

        // Fetch user's devices
        devicesViewModel.fetch(model.isLoggedIn())

        /**
         * Changing the theme from Profile -> Settings and going back to profile
         * shows the "Add Device" floating button visible again. This code is to fix this.
         */
        val navDestination = navController.currentDestination?.id
        binding.mapLayerPickerBtn.visible(navDestination == R.id.navigation_explorer)
        binding.myLocationBtn.visible(navDestination == R.id.navigation_explorer)
        binding.searchLocationBtn.visible(navDestination == R.id.navigation_home)
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
        } else {
            binding.addDevice.hide()
        }
    }

    private fun setupAuthActions() {
        binding.loginBtn.setOnClickListener {
            navigator.showLogin(this)
        }
        binding.signupBtn.setOnClickListener {
            navigator.showSignup(this)
        }
        profileViewModel.onUser().observe(this) {
            binding.authCard.visible(it == null)
        }
        profileViewModel.onLoggedOutUser().observe(this) {
            if (it && navController.currentDestination?.id == R.id.navigation_profile) {
                binding.authCard.visible(true)
            }
        }
    }

    private fun onMyLocation() {
        analytics.trackEventUserAction(AnalyticsService.ParamValue.MY_LOCATION.paramValue)
        requestLocationPermissions(this) {
            // Get last location
            explorerModel.getLocation {
                Timber.d("Got user location: $it")
                if (it == null) {
                    toast(R.string.error_claim_gps_failed)
                } else {
                    explorerModel.navigateToLocation(it, ZOOMED_IN_ZOOM_LEVEL)
                }
            }
        }
    }

    private fun onDevices(resource: Resource<List<UIDevice>>) {
        val currentDestination = navController.currentDestination?.id
        if (resource.status == Status.SUCCESS && currentDestination == R.id.navigation_devices) {
            checkForNoDevices()
        } else {
            binding.emptyContainer.visible(false)
            binding.claimRedDot.visible(false)
        }
        if (resource.data?.any { it.isOwned() } ?: false) {
            DevicesNotificationsWorker.initAndStart(this)
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
            R.id.navigation_home -> {
                binding.authCard.visible(false)
                model.getRemoteBanners()
                binding.emptyContainer.visible(false)
                binding.claimRedDot.visible(false)
                binding.addDevice.visible(false)
            }
            R.id.navigation_devices -> {
                binding.authCard.visible(false)
                if (model.isLoggedIn()) {
                    checkForNoDevices()
                    binding.addDevice.visible(true)
                } else {
                    binding.addDevice.visible(false)
                    binding.emptyContainer.visible(true)
                }
            }
            R.id.navigation_explorer -> {
                binding.authCard.visible(false)
                binding.emptyContainer.visible(false)
                binding.claimRedDot.visible(false)
                binding.addDevice.visible(false)
            }
            R.id.navigation_quests -> {
                binding.authCard.visible(false)
                binding.emptyContainer.visible(false)
                binding.claimRedDot.visible(false)
                binding.addDevice.visible(false)
            }
            else -> {
                binding.emptyContainer.visible(false)
                binding.claimRedDot.visible(false)
                binding.addDevice.visible(false)
            }
        }
        binding.searchLocationBtn.visible(destination.id == R.id.navigation_home)
        binding.mapLayerPickerBtn.visible(destination.id == R.id.navigation_explorer)
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

    private fun onExplorerSearchOpenStatus(isOpened: Boolean) {
        if (isOpened) {
            binding.mapLayerPickerBtn.hide()
            binding.myLocationBtn.hide()
        } else {
            binding.mapLayerPickerBtn.show()
            binding.myLocationBtn.show()
        }
    }

    private fun onLocationSearchOpenStatus(isOpened: Boolean) {

        if (isOpened) {
            binding.searchLocationBtn.hide()
        } else {
            binding.searchLocationBtn.show()
        }
    }

    override fun onMapDebugInfoUpdated(zoom: Double, center: Point) {
        // Do nothing
    }

    fun navView() = binding.navView
}
