package com.weatherxm.ui.networkstats.growth

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityNetworkGrowthBinding
import com.weatherxm.ui.common.Contracts.ARG_NETWORK_STATS
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.networkstats.NetworkStationStats
import com.weatherxm.ui.networkstats.NetworkStationStatsAdapter
import com.weatherxm.ui.networkstats.NetworkStats
import timber.log.Timber

class NetworkGrowthActivity : BaseActivity() {
    private lateinit var binding: ActivityNetworkGrowthBinding

    private lateinit var deployedAdapter: NetworkStationStatsAdapter
    private lateinit var manufacturedAdapter: NetworkStationStatsAdapter
    private lateinit var activeAdapter: NetworkStationStatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkGrowthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val networkStats = intent.parcelable<NetworkStats>(ARG_NETWORK_STATS)
        if (networkStats == null) {
            Timber.d("Could not start NetworkGrowthActivity. Network Stats are null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.manufacturedInfoBtn.setOnClickListener {
            openLearnMoreDialog(
                R.string.manufactured_and_provisioned,
                R.string.manufactured_weather_stations_explanation,
                AnalyticsService.ParamValue.TOTAL_STATIONS.paramValue
            )
        }

        binding.deployedInfoBtn.setOnClickListener {
            openLearnMoreDialog(
                R.string.deployed,
                R.string.deployed_weather_stations_explanation,
                AnalyticsService.ParamValue.CLAIMED_STATIONS.paramValue
            )
        }

        binding.activeInfoBtn.setOnClickListener {
            openLearnMoreDialog(
                R.string.active,
                R.string.active_weather_stations_explanation,
                AnalyticsService.ParamValue.ACTIVE_STATIONS.paramValue
            )
        }

        manufacturedAdapter = NetworkStationStatsAdapter {
            openStationShop(it, AnalyticsService.ParamValue.TOTAL.paramValue)
        }
        binding.manufacturedRecycler.adapter = manufacturedAdapter

        deployedAdapter = NetworkStationStatsAdapter {
            openStationShop(it, AnalyticsService.ParamValue.CLAIMED.paramValue)
        }
        binding.deployedRecycler.adapter = deployedAdapter

        activeAdapter = NetworkStationStatsAdapter {
            openStationShop(it, AnalyticsService.ParamValue.ACTIVE.paramValue)
        }
        binding.activeRecycler.adapter = activeAdapter

        binding.manufactured.text = networkStats.totalStations
        binding.deployed.text = networkStats.claimedStations
        binding.active.text = networkStats.activeStations

        manufacturedAdapter.submitList(networkStats.totalStationStats)
        deployedAdapter.submitList(networkStats.claimedStationStats)
        activeAdapter.submitList(networkStats.activeStationStats)

        binding.lastUpdated.text = getString(R.string.last_updated, networkStats.lastUpdated)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.NETWORK_GROWTH, classSimpleName())
    }

    private fun openStationShop(stationDetails: NetworkStationStats, categoryName: String) {
        analytics.trackEventSelectContent(
            AnalyticsService.ParamValue.OPEN_STATION_SHOP.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, categoryName),
            Pair(FirebaseAnalytics.Param.ITEM_LIST_ID, stationDetails.name)
        )
        navigator.openWebsite(this, stationDetails.url)
    }
}

