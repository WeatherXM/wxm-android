package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.TestUtils.isNoConnectionError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.NetworkError
import com.weatherxm.data.NetworkStatsResponse
import com.weatherxm.data.datasource.StatsDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class StatsRepositoryTest : BehaviorSpec({
    lateinit var dataSource: StatsDataSource
    lateinit var repo: StatsRepository
    val mockResponse = mockk<NetworkStatsResponse>()

    fun mockDataSourceCall(success: Boolean) {
        coEvery { dataSource.getNetworkStats() } returns if (success) {
            Either.Right(mockResponse)
        } else {
            Either.Left(NetworkError.NoConnectionError())
        }
    }

    beforeContainer {
        dataSource = mockk<StatsDataSource>()
        repo = StatsRepositoryImpl(dataSource)
    }

    context("Get Network Stats") {
        When("the request is successful") {
            then("return the response") {
                mockDataSourceCall(true)
                repo.getNetworkStats().isSuccess(mockResponse)
                coVerify(exactly = 1) { dataSource.getNetworkStats() }
            }
        }
        When("the request fails") {
            then("return a failure") {
                mockDataSourceCall(false)
                repo.getNetworkStats().isLeft { it.isNoConnectionError() } shouldBe true
                coVerify(exactly = 1) { dataSource.getNetworkStats() }
            }
        }
    }
})
