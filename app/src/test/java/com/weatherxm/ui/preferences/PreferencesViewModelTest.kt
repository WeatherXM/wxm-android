package com.weatherxm.ui.preferences

import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.PreferencesUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class PreferencesViewModelTest : BehaviorSpec({
    val usecase = mockk<PreferencesUseCase>()
    val authUseCase = mockk<AuthUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: PreferenceViewModel

    val installationId = "installationId"

    listener(InstantExecutorListener())

    beforeSpec {
        startKoin {
            modules(
                module {
                    single<Resources> {
                        resources
                    }
                }
            )
        }
        every { analytics.setUserProperties() } returns mutableListOf()
        justRun { analytics.setAnalyticsEnabled(any()) }
        justRun { analytics.onLogout() }
        justRun { usecase.setAnalyticsEnabled(any()) }
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
        given("the usecase which returns the response of the logout request") {
            When("it's a success") {
                coMockEitherRight({ authUseCase.logout() }, Unit)
                runTest { viewModel.logout() }
                then("the LiveData posts a success") {
                    viewModel.onLogout().isSuccess(Unit)
                }
                then("call the logout function in analytics") {
                    verify(exactly = 1) { analytics.onLogout() }
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ authUseCase.logout() }, failure)
                runTest { viewModel.logout() }
                then("the LiveData posts an error") {
                    viewModel.onLogout().isError(REACH_OUT_MSG)
                }
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
        stopKoin()
    }
})
