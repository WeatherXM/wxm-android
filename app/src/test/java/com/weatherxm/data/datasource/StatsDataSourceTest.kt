package com.weatherxm.data.datasource

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.data.models.NetworkStatsResponse
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ErrorResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk

class StatsDataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val datasource = StatsDataSourceImpl(apiService)

    val networkStats = mockk<NetworkStatsResponse>()
    val successResponse = NetworkResponse.Success<NetworkStatsResponse, ErrorResponse>(
        networkStats,
        retrofitResponse(networkStats)
    )

    context("Get network stats") {
        When("Using the Network Source") {
            testNetworkCall(
                "Network Stats",
                networkStats,
                successResponse,
                mockFunction = { apiService.getNetworkStats() },
                runFunction = { datasource.getNetworkStats() }
            )
        }
    }
})
