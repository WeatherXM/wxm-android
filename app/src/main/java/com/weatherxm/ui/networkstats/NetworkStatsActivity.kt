package com.weatherxm.ui.networkstats

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.annotation.StringRes
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityNetworkStatsBinding
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.removeLinksUnderline
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.initializeNetworkStatsChart
import me.saket.bettermovementmethod.BetterLinkMovementMethod
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

        formatTokenInfo()

        binding.buyCard.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.shop_url))
        }

        binding.buyStationBtn.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.shop_url))
            analytics.trackEventSelectContent(AnalyticsService.ParamValue.OPEN_SHOP.paramValue)
        }

        binding.contactUsBtn.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.website_contact))
            analytics.trackEventSelectContent(AnalyticsService.ParamValue.MANUFACTURER.paramValue)
        }

        setInfoButtonListeners()

        model.onNetworkStats().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    binding.empty.setVisible(false)
                    it.data?.let { data ->
                        updateUI(data)
                        binding.dataContainer.setVisible(true)
                    }
                }
                Status.ERROR -> {
                    binding.empty.animation(R.raw.anim_error, false)
                        .title(getString(R.string.error_generic_message))
                        .subtitle(it.message)
                        .action(getString(R.string.action_retry))
                        .listener { model.getNetworkStats() }
                    binding.dataContainer.setVisible(false)
                }
                Status.LOADING -> {
                    binding.dataContainer.setVisible(false)
                    binding.empty.clear()
                        .animation(R.raw.anim_loading)
                        .setVisible(true)
                }
            }
        }

        model.getNetworkStats()
    }

    private fun formatTokenInfo() {
        with(binding.tokenQuickInfo) {
            movementMethod = BetterLinkMovementMethod.newInstance().apply {
                setOnLinkClickListener { _, url ->
                    analytics.trackEventSelectContent(
                        AnalyticsService.ParamValue.TOKENOMICS.paramValue,
                        Pair(
                            FirebaseAnalytics.Param.SOURCE,
                            AnalyticsService.ParamValue.NETWORK_STATS.paramValue
                        )
                    )
                    navigator.openWebsite(this@NetworkStatsActivity, url)
                    return@setOnLinkClickListener true
                }
            }
            setHtml(R.string.token_quick_info, getString(R.string.docs_url_tokenomics))
            removeLinksUnderline()
        }
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
        binding.dataInfoBtn.setOnClickListener {
            openMessageDialog(
                R.string.data_days,
                R.string.data_days_explanation,
                AnalyticsService.ParamValue.DATA_DAYS.paramValue
            )
        }

        binding.rewardsInfoBtn.setOnClickListener {
            openMessageDialog(
                R.string.wxm_rewards,
                R.string.rewards_explanation,
                AnalyticsService.ParamValue.ALLOCATED_REWARDS.paramValue
            )
        }

        binding.earnWxmInfoBtn.setOnClickListener {
            openMessageDialog(
                null,
                R.string.average_monthly_rewards_explanation,
                AnalyticsService.ParamValue.BUY_STATION.paramValue
            )
        }

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

    @SuppressLint("SetTextI18n")
    private fun updateUI(data: NetworkStats) {
        with(binding) {
            totalDataDays30D.text = data.totalDataDays30D
            totalDataDays.text = data.totalDataDays
            lastDataDays.text = "+${data.lastDataDays}"
            dataChart.initializeNetworkStatsChart(data.dataDaysEntries)
            dataStartMonth.text = data.dataDaysStartDate
            dataEndMonth.text = data.dataDaysEndDate

            totalRewards30D.text = data.totalRewards30D
            totalRewards.text = data.totalRewards
            rewardsLastDay.text = "+${data.lastRewards}"
            rewardsChart.initializeNetworkStatsChart(data.rewardsEntries)
            rewardsStartDate.text = data.rewardsStartDate
            rewardsEndDate.text = data.rewardsEndDate

            earnWxmPerMonth.text = getString(R.string.earn_wxm, data.rewardsAvgMonthly)

            totalSupply.text = data.totalSupply
            dailyMinted.text = "+${data.dailyMinted}"
            totals.text = data.totalStations
            claimed.text = data.claimedStations
            active.text = data.activeStations

            totalsAdapter.submitList(data.totalStationStats)
            claimedAdapter.submitList(data.claimedStationStats)
            activeAdapter.submitList(data.activeStationStats)

            lastUpdated.text = getString(R.string.last_updated, data.lastUpdated)
        }
    }
}

