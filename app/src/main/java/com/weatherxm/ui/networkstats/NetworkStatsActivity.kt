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

        binding.buyCard.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.shop_url))
        }

        binding.buyStationBtn.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.shop_url))
            analytics.trackEventSelectContent(Analytics.ParamValue.OPEN_SHOP.paramValue)
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
                R.string.total_allocated_rewards,
                R.string.total_allocated_rewards_explanation,
                Analytics.ParamValue.ALLOCATED_REWARDS.paramValue
            )
        }

        binding.supplyInfoBtn.setOnClickListener {
            openMessageDialog(
                R.string.total_supply,
                R.string.total_supply_explanation,
                Analytics.ParamValue.TOTAL_SUPPLY.paramValue
            )
        }

        binding.dailyMintedInfoBtn.setOnClickListener {
            openMessageDialog(
                R.string.daily_minted,
                R.string.daily_minted_explanation,
                Analytics.ParamValue.DAILY_MINTED.paramValue
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
        @StringRes titleResId: Int,
        @StringRes messageResId: Int,
        messageSource: String
    ) {
        navigator.showMessageDialog(
            supportFragmentManager, getString(titleResId), getString(messageResId)
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
            totalDataDays.text = data.totalDataDays
            lastDataDays.text = "+${data.lastDataDays}"
            dataChart.initializeNetworkStatsChart(data.dataDaysEntries)

            totalRewards.text = data.totalRewards
            lastRewards.text = "+${data.lastRewards}"
            rewardsChart.initializeNetworkStatsChart(data.rewardsEntries)

            totalSupply.text = data.totalSupply
            dailyMinted.text = data.dailyMinted
            totals.text = data.totalStations
            claimed.text = data.claimedStations
            active.text = data.activeStations

            totalsAdapter.submitList(data.totalStationStats)
            claimedAdapter.submitList(data.claimedStationStats)
            activeAdapter.submitList(data.activeStationStats)
        }
    }
}

