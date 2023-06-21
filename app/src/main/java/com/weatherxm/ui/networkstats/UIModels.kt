package com.weatherxm.ui.networkstats

import androidx.annotation.Keep
import com.github.mikephil.charting.data.Entry
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class NetworkStats(
    val totalDataDays: String,
    val lastDataDays: String,
    val dataDaysEntries: List<Entry>,
    val totalRewards: String,
    val lastRewards: String,
    val rewardsEntries: List<Entry>,
    val totalSupply: String,
    val dailyMinted: String,
    val totalStations: String,
    val totalStationStats: List<NetworkStationStats>,
    val claimedStations: String,
    val claimedStationStats: List<NetworkStationStats>,
    val activeStations: String,
    val activeStationStats: List<NetworkStationStats>
)

@Keep
@JsonClass(generateAdapter = true)
data class NetworkStationStats(
    val name: String,
    val url: String,
    val percentage: Double,
    val amount: String
)
