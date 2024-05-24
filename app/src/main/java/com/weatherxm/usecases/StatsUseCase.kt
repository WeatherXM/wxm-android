package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.MainnetInfo
import com.weatherxm.ui.networkstats.NetworkStats

interface StatsUseCase {
    suspend fun getNetworkStats(): Either<Failure, NetworkStats>
    fun isMainnetEnabled(): Boolean
    fun getMainnetInfo(): MainnetInfo
}
