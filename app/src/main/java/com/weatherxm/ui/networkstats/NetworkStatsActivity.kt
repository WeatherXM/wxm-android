package com.weatherxm.ui.networkstats

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityNetworkStatsBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.setVisible
import com.weatherxm.util.Analytics
import com.weatherxm.util.applyInsets
import com.weatherxm.util.initializeNetworkStatsChart
import com.weatherxm.util.removeLinksUnderline
import com.weatherxm.util.setHtml
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NetworkStatsActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityNetworkStatsBinding
    private val analytics: Analytics by inject()
    private val navigator: Navigator by inject()
    private val model: NetworkStatsViewModel by viewModels()

    private lateinit var totalsAdapter: NetworkStationStatsAdapter
    private lateinit var claimedAdapter: NetworkStationStatsAdapter
    private lateinit var activeAdapter: NetworkStationStatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        totalsAdapter = NetworkStationStatsAdapter {
            openStationShop(it, Analytics.ParamValue.TOTAL.paramValue)
        }
        binding.totalsRecycler.adapter = totalsAdapter

        claimedAdapter = NetworkStationStatsAdapter {
            openStationShop(it, Analytics.ParamValue.CLAIMED.paramValue)
        }
        binding.claimedRecycler.adapter = claimedAdapter

        activeAdapter = NetworkStationStatsAdapter {
            openStationShop(it, Analytics.ParamValue.ACTIVE.paramValue)
        }
        binding.activeRecycler.adapter = activeAdapter

        formatTokenInfo()

        binding.buyCard.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.shop_url))
        }

        binding.buyStationBtn.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.shop_url))
            analytics.trackEventSelectContent(Analytics.ParamValue.OPEN_SHOP.paramValue)
        }

        binding.contactUsBtn.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.website_contact))
            analytics.trackEventSelectContent(Analytics.ParamValue.MANUFACTURER.paramValue)
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
                        Analytics.ParamValue.TOKENOMICS.paramValue,
                        Pair(
                            FirebaseAnalytics.Param.SOURCE,
                            Analytics.ParamValue.NETWORK_STATS.paramValue
                        )
                    )
                    navigator.openWebsite(this@NetworkStatsActivity, url)
                    return@setOnLinkClickListener true
                }
            }
            setHtml(R.string.token_quick_info, getString(R.string.documentation_url_tokenomics))
            removeLinksUnderline()
        }
    }

    private fun openStationShop(stationDetails: NetworkStationStats, categoryName: String) {
        analytics.trackEventSelectContent(
            Analytics.ParamValue.OPEN_STATION_SHOP.paramValue,
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
                Analytics.ParamValue.DATA_DAYS.paramValue
            )
        }

        binding.rewardsInfoBtn.setOnClickListener {
            openMessageDialog(
                R.string.wxm_rewards,
                R.string.rewards_explanation,
                Analytics.ParamValue.ALLOCATED_REWARDS.paramValue
            )
        }

        binding.earnWxmInfoBtn.setOnClickListener {
            openMessageDialog(
                null,
                R.string.average_monthly_rewards_explanation,
                Analytics.ParamValue.BUY_STATION.paramValue
            )
        }

        binding.totalInfoBtn.setOnClickListener {
            openMessageDialog(
                R.string.total_weather_stations,
                R.string.total_weather_stations_explanation,
                Analytics.ParamValue.TOTAL_STATIONS.paramValue
            )
        }

        binding.claimedInfoBtn.setOnClickListener {
            openMessageDialog(
                R.string.claimed_weather_stations,
                R.string.claimed_weather_stations_explanation,
                Analytics.ParamValue.CLAIMED_STATIONS.paramValue
            )
        }

        binding.activeInfoBtn.setOnClickListener {
            openMessageDialog(
                R.string.active_weather_stations,
                R.string.active_weather_stations_explanation,
                Analytics.ParamValue.ACTIVE_STATIONS.paramValue
            )
        }
    }

    private fun openMessageDialog(
        @StringRes titleResId: Int?,
        @StringRes messageResId: Int,
        messageSource: String
    ) {
        navigator.showMessageDialog(
            supportFragmentManager, titleResId?.let { getString(it) }, getString(messageResId)
        )
        analytics.trackEventSelectContent(
            Analytics.ParamValue.LEARN_MORE.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, messageSource)
        )
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.NETWORK_STATS,
            NetworkStatsActivity::class.simpleName
        )
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(data: NetworkStats) {
        with(binding) {
            totalDataDays30D.text = data.totalDataDays30D
            totalDataDays.text = data.totalDataDays
            lastDataDays.text = "+${data.lastDataDays}"
            dataChart.initializeNetworkStatsChart(data.dataDaysEntries)
            dataStartMonth.text = data.dataDaysStartMonth
            dataEndMonth.text = data.dataDaysEndMonth

            totalRewards30D.text = data.totalRewards30D
            totalRewards.text = data.totalRewards
            rewardsLastDay.text = "+${data.lastRewards}"
            rewardsChart.initializeNetworkStatsChart(data.rewardsEntries)
            rewardsStartMonth.text = data.rewardsStartMonth
            rewardsEndMonth.text = data.rewardsEndMonth

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

