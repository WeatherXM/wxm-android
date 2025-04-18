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
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.blockParentViewPagerOnScroll
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.ProPromotionDialogFragment
import com.weatherxm.ui.components.compose.ProPromotionCard
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
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
            model.fetchForecast(true)
        }

        initHiddenContent()

        // Initialize the adapters with empty data
        val dailyForecastAdapter = DailyForecastAdapter {
            navigator.showForecastDetails(context, model.device, it.date.toString())
        }
        val hourlyForecastAdapter = HourlyForecastAdapter {
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.HOURLY_DETAILS_CARD.paramValue,
                Pair(
                    FirebaseAnalytics.Param.ITEM_ID,
                    AnalyticsService.ParamValue.HOURLY_FORECAST.paramValue
                )
            )
            navigator.showForecastDetails(
                context,
                model.device,
                forecastSelectedISODate = it.timestamp.toISODate()
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
            model.fetchForecast(true)
        }

        model.onForecast().observe(viewLifecycleOwner) {
            hourlyForecastAdapter.submitList(it.next24Hours)
            dailyForecastAdapter.submitList(it.forecastDays)
            binding.proPromotionCard.visible(true)
            binding.dailyForecastRecycler.visible(true)
            binding.dailyForecastTitle.visible(true)
            binding.temperatureBarsInfoButton.visible(true)
            binding.hourlyForecastRecycler.visible(true)
            binding.hourlyForecastTitle.visible(true)
        }

        model.onLoading().observe(viewLifecycleOwner) {
            onLoading(it)
        }

        model.onError().observe(viewLifecycleOwner) {
            showSnackbarMessage(binding.root, it.errorMessage, it.retryFunction)
        }

        binding.proPromotionCard.setContent {
            ProPromotionCard(R.string.fine_tune_forecast) {
                analytics.trackEventSelectContent(
                    AnalyticsService.ParamValue.PRO_PROMOTION_CTA.paramValue,
                    Pair(
                        FirebaseAnalytics.Param.SOURCE,
                        AnalyticsService.ParamValue.LOCAL_FORECAST.paramValue
                    )
                )
                ProPromotionDialogFragment().show(this)
            }
        }

        fetchOrHideContent()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.DEVICE_FORECAST, classSimpleName())
    }

    private fun onLoading(isLoading: Boolean) {
        if (isLoading && binding.swiperefresh.isRefreshing) {
            binding.progress.invisible()
        } else if (isLoading) {
            binding.proPromotionCard.visible(false)
            binding.dailyForecastTitle.visible(false)
            binding.temperatureBarsInfoButton.visible(false)
            binding.hourlyForecastTitle.visible(false)
            binding.progress.visible(true)
        } else {
            binding.swiperefresh.isRefreshing = false
            binding.progress.invisible()
        }
    }

    private fun fetchOrHideContent() {
        if (model.device.relation != UNFOLLOWED) {
            binding.hiddenContentContainer.visible(false)
            binding.proPromotionCard.visible(true)
            model.fetchForecast()
        } else if (model.device.relation == UNFOLLOWED) {
            binding.proPromotionCard.visible(false)
            binding.hourlyForecastTitle.visible(false)
            binding.hourlyForecastRecycler.visible(false)
            binding.dailyForecastRecycler.visible(false)
            binding.dailyForecastTitle.visible(false)
            binding.temperatureBarsInfoButton.visible(false)
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
}
