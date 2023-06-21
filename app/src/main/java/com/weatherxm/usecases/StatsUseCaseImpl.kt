package com.weatherxm.usecases

import arrow.core.Either
import com.github.mikephil.charting.data.Entry
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkStatsStationDetails
import com.weatherxm.data.NetworkStatsTimeseries
import com.weatherxm.data.repository.StatsRepository
import com.weatherxm.ui.networkstats.NetworkStationStats
import com.weatherxm.ui.networkstats.NetworkStats
import com.weatherxm.util.NumberUtils.compactNumber
import com.weatherxm.util.NumberUtils.formatNumber
import com.weatherxm.util.Weather.EMPTY_VALUE

class StatsUseCaseImpl(private val repository: StatsRepository) : StatsUseCase {

    override suspend fun getNetworkStats(): Either<Failure, NetworkStats> {
        return repository.getNetworkStats().map { stats ->
            return@map NetworkStats(
                totalDataDays = compactNumber(stats.dataDays?.last()?.value),
                lastDataDays = getLastOfTimeseries(stats.dataDays),
                dataDaysEntries = getEntriesOfTimeseries(stats.dataDays),
                totalRewards = compactNumber(stats.tokens?.allocatedPerDay?.last()?.value),
                lastRewards = getLastOfTimeseries(stats.tokens?.allocatedPerDay),
                rewardsEntries = getEntriesOfTimeseries(stats.tokens?.allocatedPerDay),
                totalSupply = compactNumber(stats.tokens?.totalSupply),
                dailyMinted = formatNumber(stats.tokens?.dailyMinted),
                totalStations = formatNumber(stats.weatherStations.onboarded?.total),
                totalStationStats = createStationStats(stats.weatherStations.onboarded?.details),
                claimedStations = formatNumber(stats.weatherStations.claimed?.total),
                claimedStationStats = createStationStats(stats.weatherStations.claimed?.details),
                activeStations = formatNumber(stats.weatherStations.active?.total),
                activeStationStats = createStationStats(stats.weatherStations.active?.details)
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
