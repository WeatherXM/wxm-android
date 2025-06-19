package com.weatherxm.ui.networkstats

import android.os.Parcelable
import androidx.annotation.Keep
import com.github.mikephil.charting.data.Entry
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStats(
    val uptime: String,
    val netDataQualityScore: String,
    val healthActiveStations: String,
    val uptimeEntries: List<Entry>,
    val uptimeStartDate: String,
    val uptimeEndDate: String,
    val netScaleUp: String,
    val netSize: String,
    val netAddedInLast30Days: String,
    val growthEntries: List<Entry>,
    val growthStartDate: String,
    val growthEndDate: String,
    val totalRewards: String,
    val totalRewards30D: String,
    val lastRewards: String,
    val rewardsEntries: List<Entry>,
    val rewardsStartDate: String,
    val rewardsEndDate: String,
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
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStationStats(
    val name: String,
    val url: String,
    val percentage: Double,
    val amount: String
) : Parcelable
