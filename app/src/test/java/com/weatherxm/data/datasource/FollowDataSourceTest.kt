package com.weatherxm.data.datasource

import com.weatherxm.TestConfig.cacheService
import com.weatherxm.TestConfig.successUnitResponse
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.TestUtils.testThrowNotImplemented
import com.weatherxm.data.network.ApiService
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class FollowDataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val networkSource = NetworkFollowDataSource(apiService)
    val cacheSource = CacheFollowDataSource(cacheService)

    val deviceId = "deviceId"

    beforeSpec {
        every { cacheService.getFollowedDevicesIds() } returns mutableListOf()
        coJustRun { cacheService.setFollowedDevicesIds(mutableListOf(deviceId)) }
        coJustRun { cacheService.setFollowedDevicesIds(mutableListOf()) }
    }

    context("Follow a station") {
        given("A Network and a Cache Source providing the follow mechanism") {
            When("Using the Network Source") {
                testNetworkCall(
                    "Unit",
                    Unit,
                    successUnitResponse,
                    mockFunction = { apiService.followStation(deviceId) },
                    runFunction = { networkSource.followStation(deviceId) }
                )
            }
            When("Using the Cache Source") {
                then("Save the followed device in cache") {
                    cacheSource.followStation(deviceId).isSuccess(Unit)
                    verify(exactly = 1) {
                        cacheService.setFollowedDevicesIds(mutableListOf(deviceId))
                    }
                }
            }
        }
    }

    context("Unfollow a station") {
        given("A Network and a Cache Source providing the follow mechanism") {
            When("Using the Network Source") {
                testNetworkCall(
                    "Unit",
                    Unit,
                    successUnitResponse,
                    mockFunction = { apiService.unfollowStation(deviceId) },
                    runFunction = { networkSource.unfollowStation(deviceId) }
                )
            }
            When("Using the Cache Source") {
                every { cacheService.getFollowedDevicesIds() } returns mutableListOf(deviceId)
                then("Remove the unfollowed device from the cache") {
                    cacheSource.unfollowStation(deviceId).isSuccess(Unit)
                    verify(exactly = 1) { cacheService.setFollowedDevicesIds(mutableListOf()) }
                }
            }
        }
    }

    context("Get followed devices IDs and set followed devices IDs in Network Source") {
        When("Get the followed devices IDs") {
            testThrowNotImplemented { networkSource.getFollowedDevicesIds() }
        }
        When("Set the followed devices IDs") {
            testThrowNotImplemented { networkSource.setFollowedDevicesIds(mutableListOf()) }
        }
    }
})
