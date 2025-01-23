package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.NotificationsRepository
import com.weatherxm.data.repository.UserPreferencesRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AuthUseCaseTest : BehaviorSpec({
    val authRepository = mockk<AuthRepository>()
    val notificationsRepository = mockk<NotificationsRepository>()
    val userPreferenceRepo = mockk<UserPreferencesRepository>()
    val usecase = AuthUseCaseImpl(authRepository, userPreferenceRepo, notificationsRepository)

    val username = "username"
    val password = "password"
    val firstName = "firstName"
    val lastName = "lastName"
    val authToken = AuthToken("access", "refresh")

    beforeSpec {
        coJustRun { notificationsRepository.setFcmToken() }
        coJustRun { authRepository.logout() }
        coJustRun { userPreferenceRepo.setAcceptTerms() }
    }

    context("Get if a user is logged in or not") {
        given("The repository providing that information") {
            When("the user is NOT logged in") {
                every { authRepository.isLoggedIn() } returns false
                then("return false") {
                    usecase.isLoggedIn() shouldBe false
                }
            }
            When("the user is logged in") {
                every { authRepository.isLoggedIn() } returns true
                then("return true") {
                    usecase.isLoggedIn() shouldBe true
                }
            }
        }
    }

    context("Signup a user") {
        given("The repository which accepts the signup request") {
            When("the response is a failure") {
                coMockEitherLeft({ authRepository.signup(username, firstName, lastName) }, failure)
                then("return that failure") {
                    usecase.signup(username, firstName, lastName).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({ authRepository.signup(username, firstName, lastName) }, Unit)
                then("return the username") {
                    usecase.signup(username, firstName, lastName).isSuccess(username)
                }
                then("verify that the setting the acceptance of terms has taken place") {
                    verify(exactly = 1) { userPreferenceRepo.setAcceptTerms() }
                }
            }
        }
    }

    context("Login") {
        given("The repository which accepts the login request") {
            When("the response is a failure") {
                coMockEitherLeft({ authRepository.login(username, password) }, failure)
                then("return that failure") {
                    usecase.login(username, password).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight(
                    { authRepository.login(username, password) },
                    authToken
                )
                then("return the AuthToken created") {
                    usecase.login(username, password).isSuccess(authToken)
                }
                then("set the FCM token") {
                    coVerify(exactly = 1) { notificationsRepository.setFcmToken() }
                }
            }
        }
    }

    context("Reset Password") {
        given("The repository which accepts the reset password request") {
            When("the response is a failure") {
                coMockEitherLeft({ authRepository.resetPassword(username) }, failure)
                then("return that failure") {
                    usecase.resetPassword(username).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({ authRepository.resetPassword(username) }, Unit)
                then("return the user") {
                    usecase.resetPassword(username).isSuccess(Unit)
                }
            }
        }
    }

    context("Get if a password is correct or not") {
        given("The repository which returns the answer") {
            When("the response is a failure") {
                coMockEitherLeft({ authRepository.isPasswordCorrect(password) }, failure)
                then("return that failure") {
                    usecase.isPasswordCorrect(password).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({ authRepository.isPasswordCorrect(password) }, true)
                then("return the response") {
                    usecase.isPasswordCorrect(password).isSuccess(true)
                }
            }
        }
    }

    context("Logout a user") {
        given("A repository providing LOGOUT mechanism") {
            then("logout the user") {
                usecase.logout()
                coVerify(exactly = 1) { authRepository.logout() }
            }

        }
    }
})
