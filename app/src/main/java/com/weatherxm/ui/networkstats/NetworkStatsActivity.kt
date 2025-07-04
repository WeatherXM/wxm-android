package com.weatherxm.ui.networkstats

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityNetworkStatsBinding
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.ProPromotionDialogFragment
import com.weatherxm.ui.components.compose.BuyStationPromptCard
import com.weatherxm.ui.components.compose.ProPromotionCard
import org.koin.androidx.viewmodel.ext.android.viewModel

class NetworkStatsActivity : BaseActivity() {
    private lateinit var binding: ActivityNetworkStatsBinding
    private val model: NetworkStatsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.buyCard.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.shop_url))
        }

        binding.buyCard.setContent {
            BuyStationPromptCard {
                navigator.openWebsite(this, getString(R.string.shop_url))
                analytics.trackEventSelectContent(AnalyticsService.ParamValue.OPEN_SHOP.paramValue)
            }
        }

        binding.contactUsBtn.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.website_contact))
            analytics.trackEventSelectContent(AnalyticsService.ParamValue.MANUFACTURER.paramValue)
        }

        model.onNetworkStats().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    binding.empty.visible(false)
                    it.data?.let { data ->
                        updateUI(data)
                        binding.dataContainer.visible(true)
                    }
                }
                Status.ERROR -> {
                    binding.empty.animation(R.raw.anim_error, false)
                        .title(getString(R.string.error_generic_message))
                        .subtitle(it.message)
                        .action(getString(R.string.action_retry))
                        .listener { model.getNetworkStats() }
                    binding.dataContainer.visible(false)
                }
                Status.LOADING -> {
                    binding.dataContainer.visible(false)
                    binding.empty.clear()
                        .animation(R.raw.anim_loading)
                        .visible(true)
                }
            }
        }

        binding.proPromotionCard.setContent {
            ProPromotionCard(R.string.want_more_accurate_forecasts) {
                analytics.trackEventSelectContent(
                    AnalyticsService.ParamValue.PRO_PROMOTION_CTA.paramValue,
                    Pair(
                        FirebaseAnalytics.Param.SOURCE,
                        AnalyticsService.ParamValue.LOCAL_NETWORK_STATS.paramValue
                    )
                )
                ProPromotionDialogFragment().show(this)
            }
        }

        model.getNetworkStats()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.NETWORK_STATS, classSimpleName())
    }

    @Suppress("LongMethod")
    private fun updateUI(data: NetworkStats) {
        binding.healthCard
            .updateHeader(
                onInfo = {
                    openLearnMoreDialog(
                        R.string.network_health,
                        R.string.network_health_explanation,
                        AnalyticsService.ParamValue.NETWORK_HEALTH.paramValue
                    )
                }
            )
            .updateMainData(
                mainValue = data.uptime,
                chartEntries = data.uptimeEntries,
                chartDateStart = data.uptimeStartDate,
                chartDateEnd = data.uptimeEndDate
            )
            .updateFirstSubCard(
                value = data.netDataQualityScore,
                onIconClick = {
                    openLearnMoreDialog(
                        R.string.data_quality_score,
                        R.string.data_quality_score_explanation,
                        AnalyticsService.ParamValue.DATA_QUALITY_SCORE.paramValue
                    )
                }
            )
            .updateSecondSubCard(
                value = data.healthActiveStations,
                iconResId = R.drawable.ic_learn_more_info,
                onIconClick = {
                    openLearnMoreDialog(
                        R.string.active_stations,
                        R.string.active_weather_stations_explanation,
                        AnalyticsService.ParamValue.ACTIVE_STATIONS.paramValue
                    )
                }
            )

        binding.growthCard
            .updateHeader(onOpen = { navigator.showNetworkGrowth(this, data) })
            .updateMainData(
                mainValue = data.netScaleUp,
                chartEntries = data.growthEntries,
                chartDateStart = data.growthStartDate,
                chartDateEnd = data.growthEndDate
            )
            .updateFirstSubCard(data.netSize)
            .updateSecondSubCard(data.netAddedInLast30Days)

        binding.rewardsCard
            .updateHeader(
                subtitleResId = R.string.view_reward_mechanism,
                subtitleArg = getString(R.string.docs_url_reward_mechanism),
                onSubtitleClick = {
                    analytics.trackEventSelectContent(
                        AnalyticsService.ParamValue.NETWORK_STATS.paramValue,
                        Pair(
                            FirebaseAnalytics.Param.SOURCE,
                            AnalyticsService.ParamValue.REWARD_MECHANISM.paramValue
                        )
                    )
                    navigator.openWebsite(this, it)
                },
                onInfo = {
                    openLearnMoreDialog(
                        R.string.wxm_rewards,
                        R.string.rewards_explanation,
                        AnalyticsService.ParamValue.ALLOCATED_REWARDS.paramValue
                    )
                },
                onOpen = {
                    model.onNetworkStats().value?.data?.let {
                        navigator.showTokenMetrics(this, it)
                    }
                }
            )
            .updateMainData(
                mainValue = data.totalRewards30D,
                chartEntries = data.rewardsEntries,
                chartDateStart = data.rewardsStartDate,
                chartDateEnd = data.rewardsEndDate
            )
            .updateFirstSubCard(value = data.totalRewards)
            .updateSecondSubCard(
                value = "+${data.lastRewards}",
                valueTextColor = R.color.green,
                iconResId = R.drawable.ic_open_new,
                onCardClick = {
                    data.lastTxHashUrl?.let { txUrl ->
                        analytics.trackEventSelectContent(
                            AnalyticsService.ParamValue.NETWORK_STATS.paramValue,
                            Pair(
                                FirebaseAnalytics.Param.SOURCE,
                                AnalyticsService.ParamValue.LAST_RUN_HASH.paramValue
                            )
                        )
                        navigator.openWebsite(this, txUrl)
                    }
                }
            )

        binding.lastUpdated.text = getString(R.string.last_updated, data.lastUpdated)
    }
}

