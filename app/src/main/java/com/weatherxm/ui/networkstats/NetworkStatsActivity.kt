package com.weatherxm.ui.networkstats

import android.os.Bundle
import androidx.annotation.StringRes
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

    private lateinit var totalsAdapter: NetworkStationStatsAdapter
    private lateinit var claimedAdapter: NetworkStationStatsAdapter
    private lateinit var activeAdapter: NetworkStationStatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        totalsAdapter = NetworkStationStatsAdapter {
            openStationShop(it, AnalyticsService.ParamValue.TOTAL.paramValue)
        }
        binding.totalsRecycler.adapter = totalsAdapter

        claimedAdapter = NetworkStationStatsAdapter {
            openStationShop(it, AnalyticsService.ParamValue.CLAIMED.paramValue)
        }
        binding.claimedRecycler.adapter = claimedAdapter

        activeAdapter = NetworkStationStatsAdapter {
            openStationShop(it, AnalyticsService.ParamValue.ACTIVE.paramValue)
        }
        binding.activeRecycler.adapter = activeAdapter

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

        setInfoButtonListeners()

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

    private fun openStationShop(stationDetails: NetworkStationStats, categoryName: String) {
        analytics.trackEventSelectContent(
            AnalyticsService.ParamValue.OPEN_STATION_SHOP.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, categoryName),
            Pair(FirebaseAnalytics.Param.ITEM_LIST_ID, stationDetails.name)
        )
        navigator.openWebsite(this, stationDetails.url)
    }

    private fun setInfoButtonListeners() {
        binding.totalInfoBtn.setOnClickListener {
            openMessageDialog(
                R.string.total_weather_stations,
                R.string.total_weather_stations_explanation,
                AnalyticsService.ParamValue.TOTAL_STATIONS.paramValue
            )
        }

        binding.claimedInfoBtn.setOnClickListener {
            openMessageDialog(
                R.string.claimed_weather_stations,
                R.string.claimed_weather_stations_explanation,
                AnalyticsService.ParamValue.CLAIMED_STATIONS.paramValue
            )
        }

        binding.activeInfoBtn.setOnClickListener {
            openMessageDialog(
                R.string.active_weather_stations,
                R.string.active_weather_stations_explanation,
                AnalyticsService.ParamValue.ACTIVE_STATIONS.paramValue
            )
        }
    }

    private fun openMessageDialog(
        @StringRes titleResId: Int?,
        @StringRes messageResId: Int,
        messageSource: String
    ) {
        navigator.showMessageDialog(
            supportFragmentManager,
            title = titleResId?.let { getString(it) },
            message = getString(messageResId)
        )
        analytics.trackEventSelectContent(
            AnalyticsService.ParamValue.LEARN_MORE.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, messageSource)
        )
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.NETWORK_STATS, classSimpleName())
    }

    private fun updateUI(data: NetworkStats) {
        binding.growthCard
            .updateHeader(onOpen = {
                // TODO: Open Weather Stations Breakdown
            })
            .updateMainData(
                mainValue = "100",
                chartEntries = data.dataDaysEntries,
                chartDateStart = data.dataDaysStartDate,
                chartDateEnd = data.dataDaysEndDate
            )
            .updateFirstSubCard("100")
            .updateSecondSubCard("100")

        binding.rewardsCard
            .updateHeader(
                subtitleResId = R.string.view_reward_mechanism,
                subtitleArg = getString(R.string.docs_url_reward_mechanism),
                onSubtitleClick = {
                    analytics.trackEventSelectContent(
                        AnalyticsService.ParamValue.NETWORK_STATS.paramValue,
                        Pair(
                            FirebaseAnalytics.Param.SOURCE,
                            AnalyticsService.ParamValue.TOKEN_CONTRACT.paramValue
                        )
                    )
                    navigator.openWebsite(this, it)
                },
                onInfo = {
                    openMessageDialog(
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

        binding.totals.text = data.totalStations
        binding.claimed.text = data.claimedStations
        binding.active.text = data.activeStations

        totalsAdapter.submitList(data.totalStationStats)
        claimedAdapter.submitList(data.claimedStationStats)
        activeAdapter.submitList(data.activeStationStats)

        binding.lastUpdated.text = getString(R.string.last_updated, data.lastUpdated)
    }
}

