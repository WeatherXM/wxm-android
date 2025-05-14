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
