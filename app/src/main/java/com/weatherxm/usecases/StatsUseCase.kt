package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.ui.networkstats.NetworkStats

interface StatsUseCase {
    suspend fun getNetworkStats(): Either<Failure, NetworkStats>
}
