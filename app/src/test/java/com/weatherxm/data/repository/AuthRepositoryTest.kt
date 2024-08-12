package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.datasource.CacheAuthDataSource
import com.weatherxm.data.datasource.CacheUserDataSource
import com.weatherxm.data.datasource.DatabaseExplorerDataSource
import com.weatherxm.data.datasource.NetworkAuthDataSource
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.services.CacheService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.mockk

class AuthRepositoryTest : BehaviorSpec({
    lateinit var networkAuthDataSource: NetworkAuthDataSource
    lateinit var cacheAuthDataSource: CacheAuthDataSource
    lateinit var cacheUserDataSource: CacheUserDataSource
    lateinit var databaseExplorerDataSource: DatabaseExplorerDataSource
    lateinit var cacheService: CacheService
    lateinit var authRepository: AuthRepository

    val email = "email"
    val password = "password"
    val firstName = "firstName"
    val lastName = "lastName"
    val authToken = AuthToken("access", "refresh")

    beforeContainer {
        networkAuthDataSource = mockk<NetworkAuthDataSource>()
        cacheAuthDataSource = mockk<CacheAuthDataSource>()
        cacheUserDataSource = mockk<CacheUserDataSource>()
        databaseExplorerDataSource = mockk<DatabaseExplorerDataSource>()
        cacheService = mockk<CacheService>()
        authRepository = AuthRepositoryImpl(
            cacheAuthDataSource,
            networkAuthDataSource,
            cacheUserDataSource,
            databaseExplorerDataSource,
            cacheService
        )
        coJustRun { cacheUserDataSource.setUserUsername(email) }
        coJustRun { databaseExplorerDataSource.deleteAll() }
        coJustRun { networkAuthDataSource.logout(authToken.access) }
        justRun { cacheService.clearAll() }
        coMockEitherRight({ cacheUserDataSource.getUserUsername() }, email)
//        mockkStatic(Base64::class)
//        every { Base64.decode(any<String>(), any()) } returns byteArrayOf()
//        mockkConstructor(JWT::class)
//        every { JWT(authToken.access) } returns mockk()
//        every { JWT(authToken.refresh) } returns mockk()
    }

    context("Perform Auth related actions") {
        given("A Login action") {
            and("A username and a password") {
                When("login is successful") {
                    coMockEitherRight({ networkAuthDataSource.login(email, password) }, authToken)
                    then("return the AuthToken") {
                        authRepository.login(email, password).isSuccess(authToken)
                    }
                    then("save the username in the cache") {
                        coVerify(exactly = 1) { cacheUserDataSource.setUserUsername(email) }
                    }
                }
                When("login failed") {
                    coMockEitherLeft({ networkAuthDataSource.login(email, password) }, failure)
                    then("return the failure") {
                        authRepository.login(email, password).isError()
                    }
                }
            }
        }
        given("A Logout action") {
            and("auth token is in cache") {
                coMockEitherRight({ cacheService.getAuthToken() }, authToken)
                authRepository.logout()
                then("logout using the network") {
                    coVerify(exactly = 1) { networkAuthDataSource.logout(authToken.access) }
                }
                then("clear the cache and the database") {
                    coVerify(exactly = 1) { databaseExplorerDataSource.deleteAll() }
                    coVerify(exactly = 1) { cacheService.clearAll() }
                }
            }
            and("auth token is not in cache") {
                coMockEitherLeft({ cacheService.getAuthToken() }, failure)
                authRepository.logout()
                then("do NOT logout using the network") {
                    coVerify(exactly = 0) { networkAuthDataSource.logout(authToken.access) }
                }
                then("clear the cache and the database") {
                    coVerify(exactly = 1) { databaseExplorerDataSource.deleteAll() }
                    coVerify(exactly = 1) { cacheService.clearAll() }
                }
            }
        }
        given("An isLoggedIn check") {
            // TODO: Explore RoboElectric here because of JWT & Base64 mocking issues 
//            When("auth token is in cache") {
//                and("access token is valid") {
//                    coMockEitherRight({ cacheAuthDataSource.getAuthToken() }, authToken)
//                    every { authToken.isAccessTokenValid() } returns true
//                    every { authToken.isRefreshTokenValid() } returns false
//                    then("return true") {
//                        every { Base64.decode(any<String>(), any()) } returns byteArrayOf()
//                        authRepository.isLoggedIn().isSuccess(true)
//                    }
//                }
//                and("refresh token is valid") {
//                    coMockEitherRight({ cacheAuthDataSource.getAuthToken() }, authToken)
//                    every { authToken.isAccessTokenValid() } returns false
//                    every { authToken.isRefreshTokenValid() } returns true
//                    then("return true") {
//                        authRepository.isLoggedIn().isSuccess(true)
//                    }
//                }
//                and("nor access or refresh token are valid") {
//                    coMockEitherRight({ cacheAuthDataSource.getAuthToken() }, authToken)
//                    every { authToken.isRefreshTokenValid() } returns false
//                    then("return false") {
//                        authRepository.isLoggedIn().isSuccess(false)
//                    }
//                }
//            }
            When("auth token is not in cache") {
                coMockEitherLeft({ cacheAuthDataSource.getAuthToken() }, failure)
                then("return false") {
                    authRepository.isLoggedIn().isError()
                }
            }
        }
        given("A Signup action") {
            When("signup is successful") {
                coMockEitherRight(
                    { networkAuthDataSource.signup(email, firstName, lastName) },
                    Unit
                )
                then("return that Unit indicating the success") {
                    authRepository.signup(email, firstName, lastName).isSuccess(Unit)
                }
            }
            When("signup failed") {
                coMockEitherLeft(
                    { networkAuthDataSource.signup(email, firstName, lastName) },
                    failure
                )
                then("return that failure") {
                    authRepository.signup(email, firstName, lastName).isError()
                }
            }
        }
        given("A Reset Password action") {
            When("the reset prompt is successful") {
                coMockEitherRight({ networkAuthDataSource.resetPassword(email) }, Unit)
                then("return that Unit indicating the success") {
                    authRepository.resetPassword(email).isSuccess(Unit)
                }
                When("the reset prompt failed") {
                    coMockEitherLeft({ networkAuthDataSource.resetPassword(email) }, failure)
                    then("return that failure") {
                        authRepository.resetPassword(email).isError()
                    }
                }
            }
        }
        given("An isPasswordCorrect check") {
            When("we have the user's username") {
                and("we check the password correctness via a login request") {
                    When("it's a success") {
                        coMockEitherRight(
                            { networkAuthDataSource.login(email, password) },
                            authToken
                        )
                        then("return true") {
                            authRepository.isPasswordCorrect(password).isSuccess(true)
                            coVerify(exactly = 1) { networkAuthDataSource.login(email, password) }
                        }
                    }
                    When("it's a failure") {
                        coMockEitherLeft({ networkAuthDataSource.login(email, password) }, failure)
                        then("return that failure") {
                            authRepository.isPasswordCorrect(password).isError()
                            coVerify(exactly = 1) { networkAuthDataSource.login(email, password) }
                        }
                    }
                }
            }
            When("we don't have the user's username") {
                coMockEitherLeft({ cacheUserDataSource.getUserUsername() }, failure)
                then("return that failure") {
                    authRepository.isPasswordCorrect(password).isError()
                }
            }
        }
    }
})
