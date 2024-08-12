package com.weatherxm.data.repository

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.NetworkStatsResponse
import com.weatherxm.data.datasource.StatsDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coVerify
import io.mockk.mockk

class StatsRepositoryTest : BehaviorSpec({
    lateinit var dataSource: StatsDataSource
    lateinit var repo: StatsRepository
    val mockResponse = mockk<NetworkStatsResponse>()

    beforeContainer {
        dataSource = mockk<StatsDataSource>()
        repo = StatsRepositoryImpl(dataSource)
    }

    context("Get Network Stats") {
        When("the request is successful") {
            then("return the response") {
                coMockEitherRight({ dataSource.getNetworkStats() }, mockResponse)
                repo.getNetworkStats().isSuccess(mockResponse)
                coVerify(exactly = 1) { dataSource.getNetworkStats() }
            }
        }
        When("the request fails") {
            then("return a failure") {
                coMockEitherLeft({ repo.getNetworkStats() }, failure)
                repo.getNetworkStats().isError()
                coVerify(exactly = 1) { dataSource.getNetworkStats() }
            }
        }
    }
})
