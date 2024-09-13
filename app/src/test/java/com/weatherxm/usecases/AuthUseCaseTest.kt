package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.User
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.NotificationsRepository
import com.weatherxm.data.repository.UserPreferencesRepository
import com.weatherxm.data.repository.UserRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

class AuthUseCaseTest : BehaviorSpec({
    val authRepository = mockk<AuthRepository>()
    val userRepository = mockk<UserRepository>()
    val userPreferencesRepository = mockk<UserPreferencesRepository>()
    val notificationsRepository = mockk<NotificationsRepository>()
    val usecase = AuthUseCaseImpl(
        authRepository,
        userRepository,
        notificationsRepository,
        userPreferencesRepository
    )

    val username = "username"
    val password = "password"
    val firstName = "firstName"
    val lastName = "lastName"
    val user = mockk<User>()
    val authToken = AuthToken("access", "refresh")

    beforeSpec {
        coJustRun { notificationsRepository.setFcmToken() }
        every { userPreferencesRepository.shouldShowAnalyticsOptIn() } returns true
    }

    context("Get if a user is logged in or not") {
        given("The repository providing that information") {
            When("the response is a failure") {
                coMockEitherLeft({ authRepository.isLoggedIn() }, failure)
                then("return that failure") {
                    usecase.isLoggedIn().isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({ authRepository.isLoggedIn() }, true)
                then("return that success") {
                    usecase.isLoggedIn().isSuccess(true)
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

    context("Get User") {
        given("The repository which returns the user") {
            When("the response is a failure") {
                coMockEitherLeft({ userRepository.getUser() }, failure)
                then("return that failure") {
                    usecase.getUser().isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({ userRepository.getUser() }, user)
                then("return the user") {
                    usecase.getUser().isSuccess(user)
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

    context("Get if we should show the analytics opt in or not") {
        given("The repository which returns the answer") {
            then("return the answer") {
                usecase.shouldShowAnalyticsOptIn() shouldBe true
            }
        }
    }
})
