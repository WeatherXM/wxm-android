package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.UserPreferencesRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

class PreferencesUseCaseTest : BehaviorSpec({
    val authRepository = mockk<AuthRepository>()
    val appConfigRepository = mockk<AppConfigRepository>()
    val userPreferencesRepository = mockk<UserPreferencesRepository>()
    val usecase =
        PreferencesUseCaseImpl(authRepository, appConfigRepository, userPreferencesRepository)

    val analyticsEnabled = true
    val installationId = "installationId"

    beforeSpec {
        coJustRun { userPreferencesRepository.setAnalyticsEnabled(analyticsEnabled) }
        coJustRun { authRepository.logout() }
    }

    context("Get if the user is logged in") {
        given("A repository providing the answer") {
            When("it's a success") {
                coMockEitherRight({ authRepository.isLoggedIn() }, true)
                then("return that answer") {
                    usecase.isLoggedIn().isSuccess(true)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ authRepository.isLoggedIn() }, failure)
                then("return that failure") {
                    usecase.isLoggedIn().isError()
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

    context("Set if analytics are enabled or not") {
        given("A repository that provides this setting mechanism") {
            then("save the setting") {
                usecase.setAnalyticsEnabled(analyticsEnabled)
                coVerify(exactly = 1) {
                    userPreferencesRepository.setAnalyticsEnabled(analyticsEnabled)
                }
            }
        }
    }

    context("Get Installation ID") {
        given("A repository that provides the installation ID") {
            When("the installation ID is null") {
                every { appConfigRepository.getInstallationId() } returns null
                then("return null") {
                    usecase.getInstallationId() shouldBe null
                }
            }
            When("the installation ID is not null") {
                every { appConfigRepository.getInstallationId() } returns installationId
                then("return the installation ID") {
                    usecase.getInstallationId() shouldBe installationId
                }
            }
        }
    }
})
