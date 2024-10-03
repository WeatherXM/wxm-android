package com.weatherxm.data.datasource

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestConfig.successUnitResponse
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testGetFromCache
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.TestUtils.testThrowNotImplemented
import com.weatherxm.data.models.User
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ErrorResponse
import com.weatherxm.data.services.CacheService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UserDataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val cache = mockk<CacheService>()
    val networkSource = NetworkUserDataSource(apiService)
    val cacheSource = CacheUserDataSource(cache)

    val username = "username"
    val userId = "userId"
    val user = mockk<User>()

    val userResponse = NetworkResponse.Success<User, ErrorResponse>(user, retrofitResponse(user))

    beforeSpec {
        every { cache.getUserId() } returns userId
        coJustRun { cache.setUser(user) }
        coJustRun { cache.setUserUsername(username) }
    }

    context("Get user's username") {
        given("A Network and a Cache Source providing the username") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.getUserUsername() }
            }
            When("Using the Cache Source") {
                testGetFromCache(
                    "username",
                    username,
                    mockFunction = { cache.getUserUsername() },
                    runFunction = { cacheSource.getUserUsername() }
                )
            }
        }
    }

    context("Set user's username") {
        given("A Network and a Cache Source providing the SET mechanism") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.setUserUsername(username) }
            }
            When("Using the Cache Source") {
                then("save the username in cache") {
                    cacheSource.setUserUsername(username)
                    verify(exactly = 1) { cache.setUserUsername(username) }
                }
            }
        }
    }

    context("Get user") {
        given("A Network and a Cache Source providing the user") {
            When("Using the Network Source") {
                testNetworkCall(
                    "User",
                    user,
                    userResponse,
                    mockFunction = { apiService.getUser() },
                    runFunction = { networkSource.getUser() }
                )
            }
            When("Using the Cache Source") {
                testGetFromCache(
                    "username",
                    username,
                    mockFunction = { cache.getUser() },
                    runFunction = { cacheSource.getUser() }
                )
            }
        }
    }

    context("Set user") {
        given("A Network and a Cache Source providing the SET mechanism") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.setUser(user) }
            }
            When("Using the Cache Source") {
                then("save the user in cache") {
                    cacheSource.setUser(user)
                    verify(exactly = 1) { cache.setUser(user) }
                }
            }
        }
    }

    context("Get user ID") {
        given("A Network and a Cache Source providing the user ID") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.getUserId() }
            }
            When("Using the Cache Source") {
                then("return the user ID") {
                    cacheSource.getUserId() shouldBe userId
                }
            }
        }
    }

    context("Delete an account") {
        given("A Network and a Cache Source providing the DELETE mechanism") {
            When("Using the Network Source") {
                testNetworkCall(
                    "Unit",
                    Unit,
                    successUnitResponse,
                    mockFunction = { apiService.deleteAccount() },
                    runFunction = { networkSource.deleteAccount() }
                )
            }
            When("Using the Cache Source") {
                testThrowNotImplemented { cacheSource.deleteAccount() }
            }
        }
    }
})
