package com.weatherxm.ui.preferences

import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.PreferencesUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest

class PreferencesViewModelTest : BehaviorSpec({
    val usecase = mockk<PreferencesUseCase>()
    val authUseCase = mockk<AuthUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: PreferenceViewModel

    val installationId = "installationId"

    listener(InstantExecutorListener())

    beforeSpec {
        every { analytics.setUserProperties() } returns mutableListOf()
        justRun { analytics.setAnalyticsEnabled(any()) }
        justRun { analytics.onLogout() }
        justRun { usecase.setAnalyticsEnabled(any()) }
        coJustRun { authUseCase.logout() }
        every { authUseCase.isLoggedIn() } returns true
        every { usecase.getInstallationId() } returns installationId

        viewModel = PreferenceViewModel(usecase, authUseCase, analytics, dispatcher)
    }

    context("Invoke a change in SharedPreferences and update user's properties in analytics") {
        given("A change in the SharedPreferences") {
            viewModel.onPreferencesChanged.onSharedPreferenceChanged(mockk(), "")
            then("call the analytics.setUserProperties") {
                verify(exactly = 1) { analytics.setUserProperties() }
            }
        }
    }

    context("Get if the user is logged in already or not") {
        given("A use case returning the result") {
            When("it's a success") {
                then("LiveData posts a success") {
                    runTest { viewModel.isLoggedIn() }
                    viewModel.isLoggedIn() shouldBe true
                }
            }
        }
    }

    context("Perform a Logout") {
        given("Some actions to perform") {
            runTest { viewModel.logout() }
            then("call the logout function in analytics") {
                verify(exactly = 1) { analytics.onLogout() }
            }
            then("call the logout function in the usecase") {
                coVerify(exactly = 1) { authUseCase.logout() }
            }
            then("LiveData onLogout gets invoked with `true` param") {
                viewModel.onLogout().value shouldBe true
            }
        }
    }

    context("Set analytics enabled/disabled") {
        When("enable them") {
            then("enable them in the usecase and in the analytics wrapper") {
                viewModel.setAnalyticsEnabled(true)
                verify(exactly = 1) { analytics.setAnalyticsEnabled(true) }
                verify(exactly = 1) { usecase.setAnalyticsEnabled(true) }
                verify(exactly = 0) { analytics.setAnalyticsEnabled(false) }
                verify(exactly = 0) { usecase.setAnalyticsEnabled(false) }
            }
        }
        When("disable them") {
            then("disable them in the usecase and in the analytics wrapper") {
                viewModel.setAnalyticsEnabled(false)
                verify(exactly = 1) { analytics.setAnalyticsEnabled(false) }
                verify(exactly = 1) { usecase.setAnalyticsEnabled(false) }
            }
        }
    }

    context("Get the installation ID") {
        given("A use case returning that installation ID") {
            then("return the installation ID") {
                viewModel.getInstallationId() shouldBe installationId
            }
        }
    }
})
