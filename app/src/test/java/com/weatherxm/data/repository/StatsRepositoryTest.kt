package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.mockEitherLeft
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkStatsResponse
import com.weatherxm.data.datasource.StatsDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk

class StatsRepositoryTest : BehaviorSpec({
    lateinit var dataSource: StatsDataSource
    lateinit var repo: StatsRepository
    val mockResponse = mockk<NetworkStatsResponse>()
    val failure = mockk<Failure>()

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
                mockEitherLeft({ repo.getNetworkStats() }, failure)
                repo.getNetworkStats() shouldBe Either.Left(failure)
                coVerify(exactly = 1) { dataSource.getNetworkStats() }
            }
        }
    }
})
