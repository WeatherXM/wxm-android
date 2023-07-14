package com.weatherxm.usecases

import android.content.Context
import arrow.core.Either
import com.github.mikephil.charting.data.Entry
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkStatsStationDetails
import com.weatherxm.data.NetworkStatsTimeseries
import com.weatherxm.data.repository.StatsRepository
import com.weatherxm.ui.networkstats.NetworkStationStats
import com.weatherxm.ui.networkstats.NetworkStats
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.DateTimeHelper.getFormattedTime
import com.weatherxm.util.NumberUtils.compactNumber
import com.weatherxm.util.NumberUtils.formatNumber
import com.weatherxm.util.Weather.EMPTY_VALUE
import java.time.ZoneId

class StatsUseCaseImpl(
    private val repository: StatsRepository,
    private val context: Context
) : StatsUseCase {

    override suspend fun getNetworkStats(): Either<Failure, NetworkStats> {
        return repository.getNetworkStats().map { stats ->
            val timestampInUserTz = stats.lastUpdated?.withZoneSameInstant(ZoneId.systemDefault())
            val lastUpdatedDate = timestampInUserTz?.getFormattedDate(includeYear = true)
            val lastUpdatedTime = timestampInUserTz?.getFormattedTime(context)

            return@map NetworkStats(
                totalDataDays = compactNumber(stats.dataDays?.last()?.value),
                totalDataDays30D = compactNumber(
                    (stats.dataDays?.last()?.value ?: 0.0) - (stats.dataDays?.first()?.value ?: 0.0)
                ),
                lastDataDays = getLastOfTimeseries(stats.dataDays),
                dataDaysEntries = getEntriesOfTimeseries(stats.dataDays),
                dataDaysStartMonth = stats.dataDays?.first()?.ts?.getFormattedDate() ?: "",
                dataDaysEndMonth = stats.dataDays?.last()?.ts?.getFormattedDate() ?: "",
                totalRewards = compactNumber(stats.tokens?.allocatedPerDay?.last()?.value),
                totalRewards30D = compactNumber(
                    (stats.tokens?.allocatedPerDay?.last()?.value ?: 0.0)
                        - (stats.tokens?.allocatedPerDay?.first()?.value ?: 0.0)
                ),
                lastRewards = getLastOfTimeseries(stats.tokens?.allocatedPerDay),
                rewardsEntries = getEntriesOfTimeseries(stats.tokens?.allocatedPerDay),
                rewardsStartMonth = stats.tokens?.allocatedPerDay?.first()?.ts?.getFormattedDate()
                    ?: "",
                rewardsEndMonth = stats.tokens?.allocatedPerDay?.last()?.ts?.getFormattedDate()
                    ?: "",
                rewardsAvgMonthly = formatNumber(stats.tokens?.avgMonthly),
                totalSupply = compactNumber(stats.tokens?.totalSupply),
                dailyMinted = compactNumber(stats.tokens?.dailyMinted),
                totalStations = formatNumber(stats.weatherStations.onboarded?.total),
                totalStationStats = createStationStats(stats.weatherStations.onboarded?.details),
                claimedStations = formatNumber(stats.weatherStations.claimed?.total),
                claimedStationStats = createStationStats(stats.weatherStations.claimed?.details),
                activeStations = formatNumber(stats.weatherStations.active?.total),
                activeStationStats = createStationStats(stats.weatherStations.active?.details),
                lastUpdated = "$lastUpdatedDate, $lastUpdatedTime"
            )
        }
    }

    private fun getEntriesOfTimeseries(data: List<NetworkStatsTimeseries>?): List<Entry> {
        return data?.filter {
            it.value != null && it.value >= 0F
        }?.mapIndexed { index, reward ->
            Entry(index.toFloat(), reward.value?.toFloat() ?: 0F)
        } ?: mutableListOf()
    }

    private fun getLastOfTimeseries(data: List<NetworkStatsTimeseries>?): String {
        return data?.size?.let {
            if (it >= 2) {
                compactNumber(
                    (data.last().value ?: 0.0) - (data[it - 2].value ?: 0.0)
                )
            } else {
                compactNumber(data.last().value ?: 0.0)
            }
        } ?: EMPTY_VALUE
    }

    @Suppress("MagicNumber")
    private fun createStationStats(
        networkStats: List<NetworkStatsStationDetails>?
    ): List<NetworkStationStats> {
        return networkStats
            ?.map { stationStatsDetails ->
                val name = stationStatsDetails.model ?: EMPTY_VALUE
                val percentage = stationStatsDetails.percentage?.let {
                    it * 100
                } ?: 0.0
                val amount = formatNumber(stationStatsDetails.amount)
                val url = stationStatsDetails.url ?: ""
                NetworkStationStats(name, url, percentage, amount)
            }
            ?: mutableListOf()
    }
}
