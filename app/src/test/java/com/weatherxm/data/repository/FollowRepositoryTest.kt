package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
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
    val ids = listOf(validId, validId)

    beforeSpec {
        coMockEitherLeft({ networkSource.followStation(emptyId) }, failure)
        coMockEitherRight({ networkSource.followStation(validId) }, Unit)
        coMockEitherLeft({ networkSource.unfollowStation(emptyId) }, failure)
        coMockEitherRight({ networkSource.unfollowStation(validId) }, Unit)
        coJustRun { cacheSource.followStation(emptyId) }
        coJustRun { cacheSource.followStation(validId) }
        coJustRun { cacheSource.unfollowStation(emptyId) }
        coJustRun { cacheSource.unfollowStation(validId) }
        coJustRun { cacheSource.setFollowedDevicesIds(ids) }
        coEvery { cacheSource.getFollowedDevicesIds() } returns ids
    }

    context("Follow request in Repository") {
        given("a device ID") {
            When("it's invalid") {
                then("return a Failure") {
                    repo.followStation(emptyId).isError()
                    coVerify(exactly = 1) { networkSource.followStation(emptyId) }
                }
            }
            When("it's valid") {
                then("return a success") {
                    repo.followStation(validId).isSuccess(Unit)
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
                    repo.unfollowStation(emptyId).isError()
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

    context("Get/set followed devices IDs") {
        given("a list of device IDs") {
            then("Set them in cache") {
                repo.setFollowedDevicesIds(ids)
                coVerify(exactly = 1) { cacheSource.setFollowedDevicesIds(ids) }
            }
        }
        given("a request to get the device IDs of followed stations") {
            then("Get them from cache") {
                repo.getFollowedDevicesIds() shouldBe ids
                coVerify(exactly = 1) { cacheSource.getFollowedDevicesIds() }
            }
        }
    }
})
