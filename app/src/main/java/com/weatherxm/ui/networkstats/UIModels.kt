package com.weatherxm.ui.networkstats

import androidx.annotation.Keep
import com.github.mikephil.charting.data.Entry
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class NetworkStats(
    val totalDataDays: String,
    val totalDataDays30D: String,
    val lastDataDays: String,
    val dataDaysEntries: List<Entry>,
    val dataDaysStartDate: String,
    val dataDaysEndDate: String,
    val totalRewards: String,
    val totalRewards30D: String,
    val lastRewards: String,
    val rewardsEntries: List<Entry>,
    val rewardsStartDate: String,
    val rewardsEndDate: String,
    val rewardsAvgMonthly: String,
    val totalSupply: Int?,
    val circulatingSupply: Int?,
    val lastTxHashUrl: String?,
    val tokenUrl: String?,
    val rewardsUrl: String?,
    val totalStations: String,
    val totalStationStats: List<NetworkStationStats>,
    val claimedStations: String,
    val claimedStationStats: List<NetworkStationStats>,
    val activeStations: String,
    val activeStationStats: List<NetworkStationStats>,
    val lastUpdated: String
)

@Keep
@JsonClass(generateAdapter = true)
data class NetworkStationStats(
    val name: String,
    val url: String,
    val percentage: Double,
    val amount: String
)
