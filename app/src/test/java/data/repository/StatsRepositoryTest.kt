package data.repository

import arrow.core.Either
import com.weatherxm.data.ApiError
import com.weatherxm.data.NetworkError
import com.weatherxm.data.NetworkStatsResponse
import com.weatherxm.data.datasource.StatsDataSource
import com.weatherxm.data.repository.StatsRepositoryImpl
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class StatsRepositoryTest : BehaviorSpec({
    val dataSource = mockk<StatsDataSource>()
    val repo = StatsRepositoryImpl(dataSource)

    context("Get Network Stats") {
        When("the request is successful") {
            then("return the response") {
                val mockResponse = mockk<NetworkStatsResponse>()
                coEvery { dataSource.getNetworkStats() } returns Either.Right(mockResponse)
                repo.getNetworkStats() shouldBe Either.Right(mockResponse)
                coVerify(exactly = 1) { dataSource.getNetworkStats() }
            }
        }
        When("the request fails") {
            then("return a failure") {
                coEvery {
                    dataSource.getNetworkStats()
                } returns Either.Left(NetworkError.NoConnectionError())
                repo.getNetworkStats().isLeft { it is NetworkError.NoConnectionError } shouldBe true
                coVerify(exactly = 1) { dataSource.getNetworkStats() }
            }
        }
    }
})
