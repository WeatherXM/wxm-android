package com.weatherxm.ui.devicedetails.forecast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.FragmentDeviceDetailsForecastBinding
import com.weatherxm.ui.common.DeviceRelation.UNFOLLOWED
import com.weatherxm.ui.common.HourlyForecastAdapter
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.common.UILocation
import com.weatherxm.ui.common.blockParentViewPagerOnScroll
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.compose.ForecastTabSelector
import com.weatherxm.ui.components.compose.MosaicPromotionCard
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
import com.weatherxm.util.AndroidBuildInfo
import com.weatherxm.util.toISODate
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ForecastFragment : BaseFragment() {
    private lateinit var binding: FragmentDeviceDetailsForecastBinding
    private val parentModel: DeviceDetailsViewModel by activityViewModel()
    private val model: ForecastViewModel by viewModel {
        parametersOf(parentModel.device)
    }

    private lateinit var hourlyForecastAdapter: HourlyForecastAdapter
    private lateinit var dailyForecastAdapter: DailyForecastAdapter
    private var currentSelectedTab = 0
    private var hasOpenedManageSubscription = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeviceDetailsForecastBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swiperefresh.setOnRefreshListener {
            model.fetchForecasts(true)
        }

        initHiddenContent()

        // Initialize the adapters with empty data
        dailyForecastAdapter = DailyForecastAdapter {
            navigator.showForecastDetails(
                activityResultLauncher = null,
                context = context,
                device = model.device,
                location = UILocation.empty(),
                forecastSelectedISODate = it.date.toString(),
                hasFreeTrialAvailable = parentModel.hasFreePremiumTrialAvailable()
            )
        }
        hourlyForecastAdapter = HourlyForecastAdapter {
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.HOURLY_DETAILS_CARD.paramValue,
                Pair(
                    FirebaseAnalytics.Param.ITEM_ID,
                    AnalyticsService.ParamValue.HOURLY_FORECAST.paramValue
                )
            )
            navigator.showForecastDetails(
                activityResultLauncher = null,
                context = context,
                device = model.device,
                location = UILocation.empty(),
                forecastSelectedISODate = it.timestamp.toISODate(),
                hasFreeTrialAvailable = parentModel.hasFreePremiumTrialAvailable()
            )
        }
        binding.dailyForecastRecycler.adapter = dailyForecastAdapter
        binding.hourlyForecastRecycler.adapter = hourlyForecastAdapter
        binding.hourlyForecastRecycler.blockParentViewPagerOnScroll()

        binding.temperatureBarsInfoButton.setOnClickListener {
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.LEARN_MORE.paramValue,
                Pair(
                    FirebaseAnalytics.Param.ITEM_ID,
                    AnalyticsService.ParamValue.FORECAST_NEXT_7_DAYS.paramValue
                )
            )
            TemperatureBarExplanationDialogFragment().show(this)
        }

        parentModel.onFollowStatus().observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                model.device = parentModel.device
                fetchOrHideContent()
            }
        }

        parentModel.onDevicePolling().observe(viewLifecycleOwner) {
            model.device = it
        }

        parentModel.onDeviceFirstFetch().observe(viewLifecycleOwner) {
            model.device = it
            model.fetchForecasts(true)
        }

        model.onDefaultForecast().observe(viewLifecycleOwner) {
            if (currentSelectedTab == 0) {
                onForecast(it) { model.fetchForecasts(true) }
            }
        }

        model.onPremiumForecast().observe(viewLifecycleOwner) {
            if (currentSelectedTab == 1) {
                onForecast(it) { model.fetchForecasts() }
            }
        }

        // TODO: When we have the Solana implementation, remove this
        if (AndroidBuildInfo.isSolana) {
            binding.tabsOrMosaicPromptContainer.visible(false)
        }

        initForecastTabsSelector()
        initMosaicPromotionCard()
        fetchOrHideContent()
    }

    private fun initForecastTabsSelector() {
        binding.forecastTabSelector.setContent {
            ForecastTabSelector(0) { newSelectedTab ->
                currentSelectedTab = newSelectedTab
                if (newSelectedTab == 0) {
                    model.onDefaultForecast().value?.let {
                        onForecast(it) { model.fetchForecasts(true) }
                    }
                } else {
                    model.onPremiumForecast().value?.let {
                        onForecast(it) { model.fetchForecasts() }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.DEVICE_FORECAST, classSimpleName())

        if (hasOpenedManageSubscription) {
            model.fetchForecasts(true)
        }
    }

    private fun initMosaicPromotionCard() {
        binding.mosaicPromotionCard.setContent {
            MosaicPromotionCard(parentModel.hasFreePremiumTrialAvailable()) {
                hasOpenedManageSubscription = true
                navigator.showManageSubscription(
                    context,
                    parentModel.hasFreePremiumTrialAvailable(),
                    true
                )
            }
        }
    }

    private fun fetchOrHideContent() {
        if (model.device.relation != UNFOLLOWED) {
            binding.hiddenContentContainer.visible(false)
            model.fetchForecasts()
        } else if (model.device.relation == UNFOLLOWED) {
            binding.mosaicPromotionCard.visible(false)
            binding.forecastTabSelector.visible(false)
            binding.mainContainer.visible(false)
            binding.hiddenContentContainer.visible(true)
        }
    }

    private fun initHiddenContent() {
        binding.hiddenContentText.setHtml(R.string.hidden_content_prompt, model.device.name)
        binding.hiddenContentBtn.setOnClickListener {
            if (parentModel.isLoggedIn() == true) {
                if (model.device.relation == UNFOLLOWED && !model.device.isOnline()) {
                    navigator.showHandleFollowDialog(activity, true, model.device.name) {
                        parentModel.followStation()
                    }
                } else {
                    parentModel.followStation()
                }
            } else {
                navigator.showLoginDialog(
                    fragmentActivity = activity,
                    title = getString(R.string.add_favorites),
                    htmlMessage = getString(R.string.hidden_content_login_prompt, model.device.name)
                )
            }
        }
    }

    private fun onForecast(resource: Resource<UIForecast>, onErrorRetry: () -> Unit) {
        when (resource.status) {
            Status.SUCCESS -> {
                val forecast = resource.data
                hourlyForecastAdapter.submitList(forecast?.next24Hours)
                dailyForecastAdapter.setPremiumData(currentSelectedTab == 1)
                dailyForecastAdapter.submitList(forecast?.forecastDays)
                binding.poweredByMeteoblueIcon.visible(currentSelectedTab == 0)
                binding.poweredByWXMLogo.visible(currentSelectedTab == 1)
                binding.forecastTabSelector.visible(
                    forecast?.isPremium == true || currentSelectedTab == 1
                )
                binding.mosaicPromotionCard.visible(forecast?.isPremium == false)
                binding.poweredByCard.visible(true)
                binding.swiperefresh.isRefreshing = false
                binding.statusView.visible(false)
                binding.mainContainer.visible(true)
            }
            Status.ERROR -> {
                binding.mainContainer.visible(false)
                binding.statusView.animation(R.raw.anim_error, false)
                    .title(R.string.error_generic_message)
                    .action(getString(R.string.action_retry))
                    .subtitle(resource.message)
                    .listener { onErrorRetry.invoke() }
                    .visible(true)
            }
            Status.LOADING -> {
                if (binding.swiperefresh.isRefreshing) {
                    binding.statusView.visible(false)
                } else {
                    binding.mainContainer.visible(false)
                    binding.statusView.clear().animation(R.raw.anim_loading).visible(true)
                }
            }
        }
    }
}
