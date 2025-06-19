package com.weatherxm.usecases

import android.content.Context
import arrow.core.Either
import com.github.mikephil.charting.data.Entry
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.NetworkStatsStationDetails
import com.weatherxm.data.models.NetworkStatsTimeseries
import com.weatherxm.data.repository.StatsRepository
import com.weatherxm.ui.common.Contracts.EMPTY_VALUE
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.networkstats.NetworkStationStats
import com.weatherxm.ui.networkstats.NetworkStats
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.DateTimeHelper.getFormattedDateAndTime
import com.weatherxm.util.NumberUtils.compactNumber
import com.weatherxm.util.NumberUtils.formatNumber
import java.time.ZoneId

class StatsUseCaseImpl(
    private val repository: StatsRepository,
    private val context: Context
) : StatsUseCase {

    override suspend fun getNetworkStats(): Either<Failure, NetworkStats> {
        return repository.getNetworkStats().map { stats ->
            return@map NetworkStats(
                netScaleUp = "${formatNumber(stats.growth?.networkScaleUp, 1)}%",
                netSize = formatNumber(stats.growth?.networkSize),
                netAddedInLast30Days = formatNumber(stats.growth?.last30Days),
                growthEntries = getEntriesOfTimeseries(stats.growth?.last30DaysGraph),
                growthStartDate = stats.growth?.last30DaysGraph?.first()?.ts.getFormattedDate(),
                growthEndDate = stats.growth?.last30DaysGraph?.last()?.ts.getFormattedDate(),
                totalRewards = compactNumber(stats.rewards?.total),
                totalRewards30D = compactNumber(stats.rewards?.last30Days),
                lastRewards = formatNumber(stats.rewards?.lastRun),
                rewardsEntries = getEntriesOfTimeseries(stats.rewards?.last30DaysGraph),
                rewardsStartDate = stats.rewards?.last30DaysGraph?.first()?.ts.getFormattedDate(),
                rewardsEndDate = run {
                    stats.rewards?.last30DaysGraph?.size?.let {
                        if (it >= 2 && !isLastDayValid(stats.rewards.last30DaysGraph)) {
                            stats.rewards.last30DaysGraph[it - 2].ts.getFormattedDate()
                        } else {
                            stats.rewards.last30DaysGraph.last().ts.getFormattedDate()
                        }
                    } ?: String.empty()
                },
                totalSupply = stats.rewards?.tokenMetrics?.token?.totalSupply,
                circulatingSupply = stats.rewards?.tokenMetrics?.token?.circulatingSupply,
                lastTxHashUrl = stats.rewards?.lastTxHashUrl,
                tokenUrl = stats.contracts?.tokenUrl,
                rewardsUrl = stats.contracts?.rewardsUrl,
                totalStations = formatNumber(stats.weatherStations.onboarded?.total),
                totalStationStats = createStationStats(stats.weatherStations.onboarded?.details),
                claimedStations = formatNumber(stats.weatherStations.claimed?.total),
                claimedStationStats = createStationStats(stats.weatherStations.claimed?.details),
                activeStations = formatNumber(stats.weatherStations.active?.total),
                activeStationStats = createStationStats(stats.weatherStations.active?.details),
                lastUpdated = stats.lastUpdated
                    ?.withZoneSameInstant(ZoneId.systemDefault())
                    .getFormattedDateAndTime(context)
            )
        }
    }

    private fun getEntriesOfTimeseries(data: List<NetworkStatsTimeseries>?): List<Entry> {
        val filteredData = data?.filter {
            it.value != null && it.value >= 0F
        }?.mapIndexed { index, reward ->
            Entry(index.toFloat(), reward.value?.toFloat() ?: 0F)
        } ?: mutableListOf()

        val dataSize = filteredData.size
        return if (dataSize >= 2) {
            if (filteredData.last().y == filteredData[dataSize - 2].y) {
                filteredData.dropLast(1)
            } else {
                filteredData
            }
        } else {
            filteredData
        }
    }

    private fun isLastDayValid(data: List<NetworkStatsTimeseries>?): Boolean {
        return data?.size?.let {
            if (it >= 2) {
                data.last().value != data[it - 2].value
            } else {
                true
            }
        } ?: false
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
                val url = stationStatsDetails.url ?: String.empty()
                NetworkStationStats(name, url, percentage, amount)
            }
            ?: mutableListOf()
    }
}
