package com.weatherxm.usecases

import android.content.Context
import arrow.core.Either
import com.github.mikephil.charting.data.Entry
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkStatsStationDetails
import com.weatherxm.data.NetworkStatsTimeseries
import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.StatsRepository
import com.weatherxm.ui.common.MainnetInfo
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.networkstats.NetworkStationStats
import com.weatherxm.ui.networkstats.NetworkStats
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.DateTimeHelper.getFormattedTime
import com.weatherxm.util.NumberUtils.compactNumber
import com.weatherxm.util.NumberUtils.formatNumber
import com.weatherxm.util.Weather.EMPTY_VALUE
import java.time.ZoneId
import java.time.ZonedDateTime

class StatsUseCaseImpl(
    private val repository: StatsRepository,
    private val appConfigRepository: AppConfigRepository,
    private val context: Context
) : StatsUseCase {

    override suspend fun getNetworkStats(): Either<Failure, NetworkStats> {
        return repository.getNetworkStats().map { stats ->
            val timestampInUserTz = stats.lastUpdated?.withZoneSameInstant(ZoneId.systemDefault())
            val lastUpdatedDate = timestampInUserTz?.getFormattedDate(includeYear = true)
            val lastUpdatedTime = timestampInUserTz?.getFormattedTime(context)

            val dataDaysEntries = getEntriesOfTimeseries(stats.dataDays)
            val rewardEntries = getEntriesOfTimeseries(stats.tokens?.allocatedPerDay)
            return@map NetworkStats(
                totalDataDays = compactNumber(stats.dataDays?.last()?.value),
                totalDataDays30D = compactNumber(
                    (stats.dataDays?.last()?.value ?: 0.0) - (stats.dataDays?.first()?.value ?: 0.0)
                ),
                lastDataDays = getValidLastOfEntries(dataDaysEntries),
                dataDaysEntries = dataDaysEntries,
                dataDaysStartDate = stats.dataDays?.first()?.ts.getFormattedDate(),
                dataDaysEndDate = stats.dataDays?.last()?.ts.getFormattedDate(),
                totalRewards = compactNumber(stats.tokens?.totalAllocated),
                totalRewards30D = getTotalRewards30D(stats.tokens?.allocatedPerDay),
                lastRewards = getValidLastOfEntries(rewardEntries),
                rewardsEntries = rewardEntries,
                rewardsStartDate = stats.tokens?.allocatedPerDay?.first()?.ts.getFormattedDate(),
                rewardsEndDate = run {
                    stats.tokens?.allocatedPerDay?.size?.let {
                        if (it >= 2 && !isLastDayValid(stats.tokens.allocatedPerDay)) {
                            stats.tokens.allocatedPerDay[it - 2].ts.getFormattedDate()
                        } else {
                            stats.tokens.allocatedPerDay.last().ts.getFormattedDate()
                        }
                    } ?: String.empty()
                },
                rewardsAvgMonthly = formatNumber(stats.tokens?.avgMonthly),
                totalSupply = stats.tokens?.totalSupply,
                circulatingSupply = stats.tokens?.circSupply,
                latestTxHashUrl = stats.tokens?.latestTxHashUrl,
                tokenUrl = stats.contracts?.tokenUrl,
                rewardsUrl = stats.contracts?.rewardsUrl,
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

    override fun isMainnetEnabled(): Boolean {
        return appConfigRepository.isMainnetEnabled()
    }

    override fun getMainnetInfo(): MainnetInfo {
        return MainnetInfo(
            appConfigRepository.getMainnetMessage(),
            appConfigRepository.getMainnetUrl()
        )
    }

    private fun getTotalRewards30D(data: List<NetworkStatsTimeseries>?): String {
        val zoned30DaysAgo = ZonedDateTime.now().minusDays(30)
        val tokensAllocatedLast30D = data?.filter {
            it.ts?.isAfter(zoned30DaysAgo) == true && it.value != null && it.value >= 0
        }?.map {
            it.value ?: 0.0
        } ?: mutableListOf()

        return if (tokensAllocatedLast30D.size >= 2) {
            compactNumber(tokensAllocatedLast30D.last() - tokensAllocatedLast30D.first())
        } else {
            compactNumber(tokensAllocatedLast30D.last())
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

    private fun getValidLastOfEntries(data: List<Entry>): String {
        return if (data.size >= 2) {
            compactNumber(data.last().y - data[data.size - 2].y)
        } else {
            compactNumber(data.last().y)
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
