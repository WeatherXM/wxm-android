package com.weatherxm.ui.home.locations

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.google.android.material.search.SearchView.TransitionState
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.datasource.RemoteBannersDataSourceImpl.Companion.ANNOUNCEMENT_LOCAL_PRO_ACTION_URL
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.RemoteBanner
import com.weatherxm.data.models.RemoteBannerType
import com.weatherxm.data.repository.ExplorerRepositoryImpl.Companion.EXCLUDE_STATIONS
import com.weatherxm.databinding.FragmentLocationsHomeBinding
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.LocationsWeather
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UILocation
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.onTextChanged
import com.weatherxm.ui.common.setCardRadius
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.ProPromotionDialogFragment
import com.weatherxm.ui.components.compose.AnnouncementBannerView
import com.weatherxm.ui.components.compose.EmptySavedLocationsView
import com.weatherxm.ui.components.compose.InfoBannerView
import com.weatherxm.ui.home.HomeViewModel
import com.weatherxm.ui.home.devices.DevicesViewModel
import com.weatherxm.ui.home.explorer.SearchResult
import com.weatherxm.ui.home.explorer.search.NetworkSearchResultsListAdapter
import com.weatherxm.ui.home.explorer.search.NetworkSearchViewModel
import com.weatherxm.util.LocationHelper
import com.weatherxm.util.NumberUtils.formatTokens
import com.weatherxm.util.Validator.validateNetworkSearchQuery
import dev.chrisbanes.insetter.applyInsetter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class LocationsFragment : BaseFragment() {
    private val parentModel: HomeViewModel by activityViewModel()
    private val devicesModel: DevicesViewModel by activityViewModel()
    private val searchModel: NetworkSearchViewModel by viewModel()
    private val model: LocationsViewModel by viewModel()
    private lateinit var binding: FragmentLocationsHomeBinding

    private val locationHelper: LocationHelper by inject()

    private lateinit var searchAdapter: NetworkSearchResultsListAdapter
    private lateinit var savedLocationsAdapter: LocationsAdapter

    /**
     * Register the launcher for opening the forecast details to
     * refetch the data if a save/unsave takes place
     */
    private val forecastDetailsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                fetchLocationsWeather()
            }
        }

    /**
     * Suppress MissingPermission as we will try to get the location after perms are granted
     */
    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLocationsHomeBinding.inflate(inflater, container, false)

        binding.appBar.applyInsetter {
            type(statusBars = true) {
                padding(left = false, top = true, right = false, bottom = false)
            }
        }

        activity?.onBackPressedDispatcher?.addCallback(owner = this) {
            onBackPressed()
        }

        binding.swiperefresh.setOnRefreshListener {
            parentModel.getRemoteBanners()
            model.clearLocationForecastFromCache()
            fetchLocationsWeather()
        }

        binding.askForLocationCard.setOnClickListener {
            requestLocationPermissions(activity) {
                binding.askForLocationCard.visible(false)
                locationHelper.getLocationAndThen {
                    model.fetch(it, parentModel.isLoggedIn())
                }
            }
        }

        savedLocationsAdapter = LocationsAdapter {
            openForecastDetails(it.coordinates, false)
        }
        binding.savedLocations.adapter = savedLocationsAdapter

        initSearchComponents()

        devicesModel.onDevicesRewards().observe(viewLifecycleOwner) {
            onDevicesRewards(it)
        }

        observeBanners()

        model.onLocationsWeather().observe(viewLifecycleOwner) {
            updateUI(it)
        }
        binding.emptySavedLocationsCard.setContent {
            EmptySavedLocationsView()
        }

        if (!model.getSavedLocations().isNotEmpty()) {
            binding.emptySavedLocationsCard.visible(true)
        }

        fetchLocationsWeather()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.LOCATIONS_HOME, classSimpleName())

        if (locationHelper.hasLocationPermissions()) {
            binding.askForLocationCard.visible(false)
        } else {
            binding.currentLocationWeather.visible(false)
            binding.askForLocationCard.visible(true)
        }
    }

    private fun fetchLocationsWeather() {
        val hasLocationPermissions = locationHelper.hasLocationPermissions()
        val hasSavedLocations = model.getSavedLocations().isNotEmpty()
        /**
         * Suppress MissingPermission as we will try to get the location after perms are granted
         */
        @SuppressLint("MissingPermission")
        if (hasLocationPermissions) {
            locationHelper.getLocationAndThen {
                model.fetch(it, parentModel.isLoggedIn())
            }
        } else if (hasSavedLocations) {
            model.fetch(null, parentModel.isLoggedIn())
        } else {
            binding.swiperefresh.isRefreshing = false
        }
    }

    private fun onBackPressed() {
        if (binding.searchView.isShowing) {
            binding.searchView.hide()
        } else {
            activity?.finish()
        }
    }

    private fun observeBanners() {
        parentModel.onInfoBanner().observe(viewLifecycleOwner) {
            onInfoBanner(it)
        }

        parentModel.onAnnouncementBanner().observe(viewLifecycleOwner) {
            onAnnouncementBanner(it)
        }
    }

    private fun updateUI(response: Resource<LocationsWeather>) {
        when (response.status) {
            Status.SUCCESS -> {
                response.data?.current?.let {
                    binding.currentLocationWeather.setData(it) {
                        openForecastDetails(it.coordinates, true)
                    }
                    binding.currentLocationWeather.visible(true)
                }
                if (!response.data?.saved.isNullOrEmpty()) {
                    savedLocationsAdapter.submitList(response.data.saved)
                } else {
                    savedLocationsAdapter.submitList(mutableListOf())
                }
                binding.savedLocations.visible(!response.data?.saved.isNullOrEmpty())
                binding.emptySavedLocationsCard.visible(response.data?.saved.isNullOrEmpty())
                binding.swiperefresh.isRefreshing = false
                binding.statusView.visible(false)
                binding.nestedScrollView.visible(true)
            }
            Status.ERROR -> {
                binding.swiperefresh.isRefreshing = false
                binding.nestedScrollView.visible(false)
                binding.statusView.animation(R.raw.anim_error, false)
                    .title(R.string.error_generic_message)
                    .action(getString(R.string.action_try_again))
                    .subtitle(response.message)
                    .listener { fetchLocationsWeather() }
                    .visible(true)
            }
            Status.LOADING -> {
                val hasDataVisible = savedLocationsAdapter.currentList.isNotEmpty() ||
                    binding.currentLocationWeather.isVisible

                if (binding.swiperefresh.isRefreshing) {
                    binding.statusView.clear().visible(false)
                } else if (hasDataVisible) {
                    binding.statusView.clear().visible(false)
                    binding.swiperefresh.isRefreshing = true
                } else {
                    binding.nestedScrollView.visible(false)
                    binding.statusView.clear().animation(R.raw.anim_loading).visible(true)
                }
            }
        }
    }

    private fun initSearchComponents() {
        searchAdapter = NetworkSearchResultsListAdapter {
            onNetworkSearchResultClicked(it)
        }
        binding.resultsRecycler.adapter = searchAdapter

        binding.searchCard.setOnClickListener {
            binding.searchView.show()
        }

        binding.searchView.addTransitionListener { _, _, newState ->
            if (newState == TransitionState.SHOWING) {
                analytics.trackScreen(
                    AnalyticsService.Screen.LOCATION_SEARCH,
                    LocationsFragment::class.simpleName ?: String.empty()
                )
            }
        }

        // This runs only when enter is clicked
        binding.searchView.editText.setOnEditorActionListener { _, _, keyEvent ->
            // When clicking enter on keyboard ignore ACTION_UP event (as we handled ACTION_DOWN)
            if (keyEvent?.action == ACTION_UP && keyEvent.keyCode == KEYCODE_ENTER) {
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener !validateAndSearch()
        }

        binding.searchView.editText.onTextChanged {
            if (binding.searchView.currentTransitionState == TransitionState.SHOWN) {
                searchModel.setQuery(it)
                if (validateNetworkSearchQuery(it)) {
                    searchModel.networkSearch(exclude = EXCLUDE_STATIONS)
                    return@onTextChanged
                } else if (it.isEmpty()) {
                    searchAdapter.updateData(it)
                }
                searchModel.cancelNetworkSearchJob(false)
                binding.searchProgress.invisible()
            }
        }

        searchModel.onSearchResults().observe(viewLifecycleOwner) {
            onSearchResults(it)
        }
    }

    private fun onInfoBanner(infoBanner: RemoteBanner?) {
        if (infoBanner != null) {
            binding.infoBanner.setContent {
                InfoBannerView(
                    title = infoBanner.title,
                    subtitle = infoBanner.message,
                    actionLabel = infoBanner.actionLabel,
                    showActionButton = infoBanner.showActionButton,
                    showCloseButton = infoBanner.showCloseButton,
                    onAction = {
                        analytics.trackEventSelectContent(
                            AnalyticsService.ParamValue.INFO_BANNER_BUTTON.paramValue,
                            Pair(FirebaseAnalytics.Param.ITEM_ID, infoBanner.url)
                        )
                        navigator.openWebsite(context, infoBanner.url)
                    },
                    onClose = {
                        parentModel.dismissRemoteBanner(RemoteBannerType.INFO_BANNER, infoBanner.id)
                        binding.contentContainerCard.setCardRadius(0F, 0F, 0F, 0F)
                        binding.infoBanner.visible(false)
                    }
                )
            }
            binding.infoBanner.visible(true)
            val radius = resources.getDimension(R.dimen.radius_large)
            binding.contentContainerCard.setCardRadius(radius, radius, 0F, 0F)
        } else if (binding.infoBanner.isVisible) {
            binding.infoBanner.visible(false)
            binding.contentContainerCard.setCardRadius(0F, 0F, 0F, 0F)
        }
    }

    private fun onAnnouncementBanner(announcementBanner: RemoteBanner?) {
        announcementBanner?.let {
            binding.announcementBanner.setContent {
                AnnouncementBannerView(
                    title = it.title,
                    subtitle = it.message,
                    actionLabel = it.actionLabel,
                    showActionButton = it.showActionButton,
                    showCloseButton = it.showCloseButton,
                    onAction = {
                        analytics.trackEventSelectContent(
                            AnalyticsService.ParamValue.ANNOUNCEMENT_CTA.paramValue,
                            Pair(FirebaseAnalytics.Param.ITEM_ID, it.url),
                        )
                        if (it.url == ANNOUNCEMENT_LOCAL_PRO_ACTION_URL) {
                            analytics.trackEventSelectContent(
                                AnalyticsService.ParamValue.PRO_PROMOTION_CTA.paramValue,
                                Pair(FirebaseAnalytics.Param.ITEM_ID, it.url),
                                Pair(
                                    FirebaseAnalytics.Param.SOURCE,
                                    AnalyticsService.ParamValue.REMOTE_DEVICES_LIST.paramValue
                                )
                            )
                            ProPromotionDialogFragment().show(this)
                        } else {
                            navigator.openWebsite(context, it.url)
                        }
                    },
                    onClose = {
                        parentModel.dismissRemoteBanner(RemoteBannerType.ANNOUNCEMENT, it.id)
                        binding.announcementBanner.visible(false)
                    }
                )
            }
            binding.announcementBanner.visible(true)
        } ?: binding.announcementBanner.visible(false)
    }

    private fun onDevicesRewards(rewards: DevicesRewards) {
        binding.totalEarnedCard.visible(true)
        binding.totalEarnedCard.setOnClickListener {
            analytics.trackEventUserAction(
                AnalyticsService.ParamValue.TOKENS_EARNED_PRESSED.paramValue
            )
            navigator.showDevicesRewards(this, rewards)
        }
        binding.stationRewards.text = getString(R.string.wxm_amount, formatTokens(rewards.total))
        binding.totalEarnedContainer.visible(rewards.total > 0F)
        binding.noRewardsYet.visible(rewards.devices.isNotEmpty() && rewards.total == 0F)
        binding.ownDeployEarn.visible(rewards.devices.isEmpty() && rewards.total == 0F)
    }

    // Returns if validation was a success and search API call has been processed
    private fun validateAndSearch(): Boolean {
        val query = binding.searchView.text.toString()
        return if (validateNetworkSearchQuery(query)) {
            searchModel.networkSearch(true)
            true
        } else {
            context?.toast(R.string.network_search_validation_error)
            false
        }
    }

    private fun onSearchResults(resource: Resource<List<SearchResult>>) {
        when (resource.status) {
            Status.SUCCESS -> {
                binding.searchResultsStatusView.visible(false)
                if (resource.data.isNullOrEmpty()) {
                    binding.resultsRecycler.visible(false)
                    binding.searchEmptyResultsContainer.visible(true)
                } else {
                    binding.searchEmptyResultsContainer.visible(false)
                    binding.resultsRecycler.visible(true)
                    searchAdapter.updateData(binding.searchView.text.toString(), resource.data)
                }
                binding.searchProgress.invisible()
            }
            Status.ERROR -> {
                binding.searchProgress.invisible()
                binding.resultsRecycler.visible(false)
                binding.searchEmptyResultsContainer.visible(false)
                binding.searchResultsStatusView
                    .clear()
                    .animation(R.raw.anim_error)
                    .title(getString(R.string.search_error))
                    .subtitle(resource.message)
                    .action(getString(R.string.action_retry))
                    .listener {
                        validateAndSearch()
                    }
                    .visible(true)
            }
            Status.LOADING -> {
                binding.searchResultsStatusView.visible(false)
                binding.searchEmptyResultsContainer.visible(false)
                binding.searchProgress.visible(true)
            }
        }
    }

    private fun onNetworkSearchResultClicked(result: SearchResult) {
        binding.searchView.hide()

        analytics.trackEventSelectContent(
            AnalyticsService.ParamValue.CLICK_ON_LOCATION_SEARCH_RESULT.paramValue
        )

        result.center?.let {
            openForecastDetails(it, false)
        }
    }

    private fun openForecastDetails(coordinates: Location, isCurrentLocation: Boolean) {
        navigator.showForecastDetails(
            activityResultLauncher = forecastDetailsLauncher,
            context = context,
            device = UIDevice.empty(),
            location = UILocation(
                coordinates = coordinates,
                isCurrentLocation = isCurrentLocation,
                isSaved = model.isLocationSaved(coordinates)
            )
        )
    }
}
