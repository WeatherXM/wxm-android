package com.weatherxm.ui.home.locations

import android.os.Bundle
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isVisible
import com.google.android.material.search.SearchView.TransitionState
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.datasource.RemoteBannersDataSourceImpl.Companion.ANNOUNCEMENT_LOCAL_PRO_ACTION_URL
import com.weatherxm.data.models.RemoteBanner
import com.weatherxm.data.models.RemoteBannerType
import com.weatherxm.data.repository.ExplorerRepositoryImpl.Companion.EXCLUDE_STATIONS
import com.weatherxm.databinding.FragmentLocationsHomeBinding
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
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
import com.weatherxm.util.Validator
import dev.chrisbanes.insetter.applyInsetter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class LocationsFragment : BaseFragment() {
    private val parentModel: HomeViewModel by activityViewModel()
    private val devicesModel: DevicesViewModel by activityViewModel()
    private val searchModel: NetworkSearchViewModel by viewModel()
    private lateinit var binding: FragmentLocationsHomeBinding

    private val locationHelper: LocationHelper by inject()

    private lateinit var searchAdapter: NetworkSearchResultsListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLocationsHomeBinding.inflate(inflater, container, false)

        binding.root.applyInsetter {
            type(statusBars = true) {
                padding(left = false, top = true, right = false, bottom = false)
            }
        }

        activity?.onBackPressedDispatcher?.addCallback(owner = this) {
            onBackPressed()
        }

        binding.swiperefresh.setOnRefreshListener {
            parentModel.getRemoteBanners()
        }

        binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            parentModel.onScroll(scrollY - oldScrollY)
        }

        binding.askForLocationCard.setOnClickListener {
            requestLocationPermissions(activity) {
                binding.askForLocationCard.visible(false)
                // TODO: Fetch the weather for current location
                binding.currentLocationWeather.setData()
                binding.currentLocationWeather.visible(true)
            }
        }

        binding.emptySavedLocationsCard.setContent {
            EmptySavedLocationsView()
        }
        // TODO: Show the below only if saved locations are empty
        binding.emptySavedLocationsCard.visible(true)

        initSearchComponents()

        devicesModel.onDevicesRewards().observe(viewLifecycleOwner) {
            onDevicesRewards(it)
        }

        parentModel.onInfoBanner().observe(viewLifecycleOwner) {
            onInfoBanner(it)
        }

        parentModel.onAnnouncementBanner().observe(viewLifecycleOwner) {
            onAnnouncementBanner(it)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        if (locationHelper.hasLocationPermissions()) {
            binding.askForLocationCard.visible(false)
            // TODO: Fetch the weather for current location
            binding.currentLocationWeather.setData()
            binding.currentLocationWeather.visible(true)
        } else {
            binding.askForLocationCard.visible(true)
        }
    }

    private fun onBackPressed() {
        if (binding.searchView.isShowing) {
            binding.searchView.hide()
        } else {
            activity?.finish()
        }
    }

    private fun initSearchComponents() {
        binding.searchCard.setOnClickListener {
            binding.searchView.show()
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
                if (Validator.validateNetworkSearchQuery(it)) {
                    searchModel.networkSearch(exclude = EXCLUDE_STATIONS)
                    return@onTextChanged
                }
                searchModel.cancelNetworkSearchJob()
                binding.searchProgress.invisible()
            }
        }

        searchModel.onSearchResults().observe(viewLifecycleOwner) {
            onSearchResults(it)
        }

        searchAdapter = NetworkSearchResultsListAdapter {
            onNetworkSearchResultClicked(it)
        }
        binding.resultsRecycler.adapter = searchAdapter
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
        return if (Validator.validateNetworkSearchQuery(query)) {
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
                    binding.searchEmptyResultsTitle.text = getString(R.string.search_no_results)
                    binding.searchEmptyResultsDesc.text =
                        getString(R.string.search_no_results_message)
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

    private fun onNetworkSearchResultClicked(networkSearchResult: SearchResult) {
        binding.searchView.hide()
        searchModel.setQuery(String.empty())
        // TODO: Open forecast details for this search result
    }
}
