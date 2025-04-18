package com.weatherxm.ui.devicehistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.FragmentHistoryChartsBinding
import com.weatherxm.ui.common.Charts
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.setDisplayTimezone
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.ProPromotionDialogFragment
import com.weatherxm.ui.components.compose.ProPromotionCard
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber
import java.time.format.DateTimeFormatter.ofLocalizedDate
import java.time.format.FormatStyle

class HistoryChartsFragment : BaseFragment() {

    fun interface SwipeRefreshCallback {
        fun onSwipeRefresh()
    }

    private val model: HistoryViewModel by activityViewModel()
    private var callback: SwipeRefreshCallback? = null
    private lateinit var binding: FragmentHistoryChartsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            callback = activity as SwipeRefreshCallback
        } catch (e: ClassCastException) {
            Timber.w(e, "${activity?.localClassName} does not implement SwipeRefreshCallback")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHistoryChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.displayTimeNotice.setDisplayTimezone(model.device.timezone)

        binding.swiperefresh.setOnRefreshListener {
            callback?.onSwipeRefresh()
        }

        model.onNewDate().observe(viewLifecycleOwner) {
            binding.chartsContainer.scrollTo(0, 0)
        }

        model.charts().observe(viewLifecycleOwner) { resource ->
            Timber.d("Charts updated: ${resource.status}")
            onCharts(resource)
        }

        binding.proPromotionCard.setContent {
            ProPromotionCard(R.string.unlock_full_weather_history) {
                analytics.trackEventSelectContent(
                    AnalyticsService.ParamValue.PRO_PROMOTION_CTA.paramValue,
                    Pair(
                        FirebaseAnalytics.Param.SOURCE,
                        AnalyticsService.ParamValue.LOCAL_HISTORY.paramValue
                    )
                )
                ProPromotionDialogFragment().show(this)
            }
        }
    }

    private fun onCharts(resource: Resource<Charts>) {
        when (resource.status) {
            Status.SUCCESS -> {
                binding.swiperefresh.isRefreshing = false
                val data = resource.data
                if (data == null || data.isEmpty()) {
                    binding.chartsContainer.visible(false)
                    binding.empty.clear()
                        .title(getString(R.string.empty_history_day_title))
                        .subtitle(
                            resource.data?.date?.let {
                                getString(
                                    R.string.empty_history_day_subtitle_with_day,
                                    it.format(ofLocalizedDate(FormatStyle.MEDIUM))
                                )
                            } ?: getString(R.string.empty_history_day_subtitle)
                        )
                        .animation(R.raw.anim_empty_generic)
                        .visible(true)
                } else {
                    Timber.d("Updating charts for ${data.date}")
                    binding.charts.clearCharts()
                    binding.charts.initTemperatureChart(data.temperature, data.feelsLike)
                    binding.charts.initWindChart(
                        data.windSpeed, data.windGust, data.windDirection
                    )
                    binding.charts.initPrecipitationChart(
                        data.precipitation, data.precipitationAccumulated, true
                    )
                    binding.charts.initHumidityChart(data.humidity)
                    binding.charts.initPressureChart(data.pressure)
                    binding.charts.initSolarChart(data.uv, data.solarRadiation)
                    if (model.isTodayShown()) {
                        // Auto highlight latest entry
                        val latestEntry = binding.charts.getLatestChartEntry(data.temperature)
                        binding.charts.autoHighlightCharts(latestEntry)
                    } else {
                        // Auto highlight past dates on 00:00
                        binding.charts.autoHighlightCharts(0F)
                    }
                    binding.empty.visible(false)
                    binding.chartsContainer.visible(true)
                }
            }
            Status.ERROR -> {
                Timber.d("Got error: $resource.message")
                binding.swiperefresh.isRefreshing = false
                binding.chartsContainer.visible(false)
                binding.empty.clear()
                    .animation(R.raw.anim_error)
                    .title(getString(R.string.error_history_no_data_on_day))
                    .subtitle(resource.message)
                    .action(getString(R.string.action_retry))
                    .listener { callback?.onSwipeRefresh() }
                    .visible(true)
            }
            Status.LOADING -> {
                if (binding.swiperefresh.isRefreshing) {
                    binding.empty.clear().visible(false)
                } else {
                    binding.chartsContainer.visible(false)
                    binding.empty.clear().animation(R.raw.anim_loading).visible(true)
                }
            }
        }
    }
}
