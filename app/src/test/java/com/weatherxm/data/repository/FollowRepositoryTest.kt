package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.ApiError
import com.weatherxm.data.datasource.CacheFollowDataSource
import com.weatherxm.data.datasource.NetworkFollowDataSource
import com.weatherxm.data.repository.FollowRepositoryImpl
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk

class FollowRepositoryTest : BehaviorSpec({
    val networkSource = mockk<NetworkFollowDataSource>()
    val cacheSource = mockk<CacheFollowDataSource>()
    val repo = FollowRepositoryImpl(networkSource, cacheSource)

    beforeSpec {
        coEvery { networkSource.followStation("") } returns Either.Left(ApiError.DeviceNotFound(""))
        coEvery { networkSource.followStation("testId") } returns Either.Right(Unit)
        coEvery { networkSource.unfollowStation("") } returns Either.Left(ApiError.DeviceNotFound(""))
        coEvery { networkSource.unfollowStation("testId") } returns Either.Right(Unit)
        coJustRun { cacheSource.followStation("") }
        coJustRun { cacheSource.followStation("testId") }
        coJustRun { cacheSource.unfollowStation("") }
        coJustRun { cacheSource.unfollowStation("testId") }
    }

    context("Follow request in Repository") {
        given("a device ID") {
            When("it's invalid") {
                then("return a Failure") {
                    repo.followStation("").isLeft { it is ApiError.DeviceNotFound } shouldBe true
                    coVerify(exactly = 1) { networkSource.followStation("") }
                }
            }
            When("it's valid") {
                then("return a success") {
                    repo.followStation("testId") shouldBe Either.Right(Unit)
                    coVerify(exactly = 1) { networkSource.followStation("testId") }
                }
                and("Save it in the cache") {
                    coVerify(exactly = 1) { cacheSource.followStation("testId") }
                }
            }
        }
    }

    context("Unfollow request in repository") {
        given("a device ID") {
            When("it's invalid") {
                then("return a Failure") {
                    repo.unfollowStation("").isLeft { it is ApiError.DeviceNotFound } shouldBe true
                    coVerify(exactly = 1) { networkSource.unfollowStation("") }
                }
                and("Re-save it back in the cache") {
                    coVerify(exactly = 1) { cacheSource.followStation("") }
                }
            }
            When("it's valid") {
                then("return a success") {
                    repo.unfollowStation("testId") shouldBe Either.Right(Unit)
                    coVerify(exactly = 1) { networkSource.unfollowStation("testId") }
                }
                and("Remove it from the cache") {
                    coVerify(exactly = 1) { cacheSource.unfollowStation("testId") }
                }
            }
        }
    }
})
