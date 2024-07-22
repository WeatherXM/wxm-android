package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.TestUtils.isDeviceNotFound
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.ApiError
import com.weatherxm.data.datasource.CacheFollowDataSource
import com.weatherxm.data.datasource.NetworkFollowDataSource
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

    val validId = "testId"
    val emptyId = ""

    beforeSpec {
        coEvery {
            networkSource.followStation(emptyId)
        } returns Either.Left(ApiError.DeviceNotFound(""))
        coEvery { networkSource.followStation(validId) } returns Either.Right(Unit)
        coEvery {
            networkSource.unfollowStation(emptyId)
        } returns Either.Left(ApiError.DeviceNotFound(""))
        coEvery { networkSource.unfollowStation(validId) } returns Either.Right(Unit)
        coJustRun { cacheSource.followStation(emptyId) }
        coJustRun { cacheSource.followStation(validId) }
        coJustRun { cacheSource.unfollowStation(emptyId) }
        coJustRun { cacheSource.unfollowStation(validId) }
    }

    context("Follow request in Repository") {
        given("a device ID") {
            When("it's invalid") {
                then("return a Failure") {
                    repo.followStation(emptyId).isLeft { it.isDeviceNotFound()} shouldBe true
                    coVerify(exactly = 1) { networkSource.followStation(emptyId) }
                }
            }
            When("it's valid") {
                then("return a success") {
                    repo.followStation(validId) shouldBe Either.Right(Unit)
                    coVerify(exactly = 1) { networkSource.followStation(validId) }
                }
                and("Save it in the cache") {
                    coVerify(exactly = 1) { cacheSource.followStation(validId) }
                }
            }
        }
    }

    context("Unfollow request in repository") {
        given("a device ID") {
            When("it's invalid") {
                then("return a Failure") {
                    repo.unfollowStation(emptyId).isLeft { it.isDeviceNotFound() } shouldBe true
                    coVerify(exactly = 1) { networkSource.unfollowStation(emptyId) }
                }
                and("Re-save it back in the cache") {
                    coVerify(exactly = 1) { cacheSource.followStation(emptyId) }
                }
            }
            When("it's valid") {
                then("return a success") {
                    repo.unfollowStation(validId).isSuccess(Unit)
                    coVerify(exactly = 1) { networkSource.unfollowStation(validId) }
                }
                and("Remove it from the cache") {
                    coVerify(exactly = 1) { cacheSource.unfollowStation(validId) }
                }
            }
        }
    }
})
