package com.weatherxm.ui.preferences

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.usecases.PreferencesUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesViewModelTest : BehaviorSpec({
    val usecase = mockk<PreferencesUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: PreferenceViewModel

    val installationId = "installationId"

    listener(InstantExecutorListener())
    Dispatchers.setMain(StandardTestDispatcher())

    beforeSpec {
        every { analytics.setUserProperties() } returns mutableListOf()
        justRun { analytics.setAnalyticsEnabled(any()) }
        justRun { analytics.onLogout() }
        justRun { usecase.setAnalyticsEnabled(any()) }
        coJustRun { usecase.logout() }
        coMockEitherRight({ usecase.isLoggedIn() }, true)
        every { usecase.getInstallationId() } returns installationId

        viewModel = PreferenceViewModel(usecase, analytics)
    }

    context("Invoke a change in SharedPreferences in order to call the analytics.setUserProperties") {
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
                    viewModel.isLoggedIn().value?.isSuccess(true)
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
                coVerify(exactly = 1) { usecase.logout() }
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

    afterSpec {
        Dispatchers.resetMain()
    }
})
