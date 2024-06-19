package com.weatherxm.ui.devicedetails.forecast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentDeviceDetailsForecastBinding
import com.weatherxm.ui.common.DeviceRelation.UNFOLLOWED
import com.weatherxm.ui.common.HourlyForecastAdapter
import com.weatherxm.ui.common.blockParentViewPagerOnScroll
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
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
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.DAILY_CARD.paramValue,
                Pair(
                    FirebaseAnalytics.Param.ITEM_ID,
                    AnalyticsService.ParamValue.DAILY_FORECAST.paramValue
                )
            )
            navigator.showForecastDetails(
                context,
                model.device,
                forecastSelectedISODate = it.date.toString()
            )
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

        parentModel.onFollowStatus().observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                model.device = parentModel.device
                fetchOrHideContent()
            }
        }

        parentModel.onDeviceFirstFetch().observe(viewLifecycleOwner) {
            model.device = it
            model.fetchForecast(true)
        }

        model.onForecast().observe(viewLifecycleOwner) {
            hourlyForecastAdapter.submitList(it.next24Hours)
            dailyForecastAdapter.submitList(it.forecastDays)
            binding.dailyForecastRecycler.visible(true)
            binding.dailyForecastTitle.visible(true)
            binding.hourlyForecastRecycler.visible(true)
            binding.hourlyForecastTitle.visible(true)
        }

        model.onLoading().observe(viewLifecycleOwner) {
            if (it && binding.swiperefresh.isRefreshing) {
                binding.progress.invisible()
            } else if (it) {
                binding.dailyForecastTitle.visible(false)
                binding.hourlyForecastTitle.visible(false)
                binding.progress.visible(true)
            } else {
                binding.swiperefresh.isRefreshing = false
                binding.progress.invisible()
            }
        }

        model.onError().observe(viewLifecycleOwner) {
            showSnackbarMessage(binding.root, it.errorMessage, it.retryFunction)
        }

        fetchOrHideContent()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.DEVICE_FORECAST, classSimpleName())
    }

    private fun fetchOrHideContent() {
        if (model.device.relation != UNFOLLOWED) {
            binding.hiddenContentContainer.visible(false)
            model.fetchForecast()
        } else if (model.device.relation == UNFOLLOWED) {
            binding.dailyForecastRecycler.visible(false)
            binding.dailyForecastTitle.visible(false)
            binding.hourlyForecastTitle.visible(false)
            binding.hourlyForecastRecycler.visible(false)
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
