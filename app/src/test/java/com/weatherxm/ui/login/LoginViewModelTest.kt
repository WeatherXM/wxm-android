package com.weatherxm.ui.login

import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
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
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest : BehaviorSpec({
    val usecase = mockk<AuthUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    val viewModel = LoginViewModel(usecase, resources, analytics)
    val email = "email"
    val invalidUsername = "Invalid Username"
    val invalidUsernameFailure = ApiError.AuthError.InvalidUsername("")

    listener(InstantExecutorListener())
    Dispatchers.setMain(StandardTestDispatcher())

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
        justRun { analytics.trackEventFailure(any()) }
        coMockEitherRight({ usecase.isLoggedIn() }, true)
        every { usecase.shouldShowAnalyticsOptIn() } returns true
    }

    context("Get if the user is logged in already or not") {
        given("A use case returning the result") {
            When("it's a success") {
                then("LiveData posts a success") {
                    runTest { viewModel.isLoggedIn() }
                    viewModel.isLoggedIn().value?.isSuccess(true)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ usecase.isLoggedIn() }, failure)
                then("LiveData posts an error") {
                    runTest { viewModel.isLoggedIn() }
                    viewModel.isLoggedIn().value?.isError()
                }
            }
        }
    }

    context("Get if we should show the analytics opt-in screen or not") {
        given("A use case returning the result") {
            then("return that result") {
                viewModel.shouldShowAnalyticsOptIn() shouldBe true
            }
        }
    }

    context("A Login request") {
        given("A use case accepting that login request") {
            When("it's a success") {

            }
            When("it's a failure") {
                and("It's an InvalidUsername failure") {

                }
                and("It's an InvalidPassword failure") {

                }
                and("It's an InvalidCredentials failure") {

                }
                and("it's any other failure") {

                }
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
        stopKoin()
    }
})
