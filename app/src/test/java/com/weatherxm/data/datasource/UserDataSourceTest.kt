package com.weatherxm.data.datasource

import arrow.core.Either
import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestConfig.cacheService
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.successUnitResponse
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testGetFromCache
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.TestUtils.testThrowNotImplemented
import com.weatherxm.data.models.User
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ErrorResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UserDataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val networkSource = NetworkUserDataSource(apiService)
    val cacheSource = CacheUserDataSource(cacheService)

    val username = "username"
    val userId = "userId"
    val user = mockk<User>()

    val userResponse = NetworkResponse.Success<User, ErrorResponse>(user, retrofitResponse(user))

    beforeSpec {
        every { cacheService.getUserId() } returns userId
        coJustRun { cacheService.setUser(user) }
        coJustRun { cacheService.setUserUsername(username) }
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
                    mockFunction = { cacheService.getUserUsername() },
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
                then("save the username in cacheService") {
                    cacheSource.setUserUsername(username)
                    verify(exactly = 1) { cacheService.setUserUsername(username) }
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
                /**
                 * Avoid using testGetFromCache because we get an invalid warning, more info:
                 * https://github.com/mockk/mockk/issues/1291
                 *
                 * So we use property-backing fields: https://mockk.io/#property-backing-fields
                 */
                and("the response is a success") {
                    every {
                        cacheService.getUser()
                    } propertyType Either::class answers { Either.Right(user) }
                    then("return the user") {
                        cacheSource.getUser().isSuccess(user)
                    }
                }
                and("the response is a failure") {
                    every {
                        cacheService.getUser()
                    } propertyType Either::class answers { Either.Left(failure) }
                    then("return the failure") {
                        cacheSource.getUser().isError()
                    }
                }
            }
        }
    }

    context("Set user") {
        given("A Network and a Cache Source providing the SET mechanism") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.setUser(user) }
            }
            When("Using the Cache Source") {
                then("save the user in cacheService") {
                    cacheSource.setUser(user)
                    verify(exactly = 1) { cacheService.setUser(user) }
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
