package com.weatherxm.ui.passwordprompt

import arrow.core.Either
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.ApiError
import com.weatherxm.data.Resource
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.usecases.PasswordPromptUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
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
class PasswordPromptViewModelTest : BehaviorSpec({
    val usecase = mockk<PasswordPromptUseCase>()
    val resources = mockk<Resources>()
    val analytics = mockk<AnalyticsWrapper>()
    val viewModel = PasswordPromptViewModel(usecase, resources, analytics)

    listener(InstantExecutorListener())

    startKoin {
        modules(
            module {
                single<Resources> {
                    resources
                }
            }
        )
    }

    beforeSpec {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    beforeTest {
        every { resources.getString(R.string.error_invalid_password) } returns "Invalid Password"
        every { analytics.trackEventFailure(any()) } just Runs
        coEvery { usecase.isPasswordCorrect("testValid") } returns Either.Right(Unit)

        val failure = ApiError.AuthError.LoginError.InvalidPassword("")
        coEvery { usecase.isPasswordCorrect("testInvalid") } returns Either.Left(failure)
        every {
            failure.getDefaultMessage(R.string.error_invalid_password)
        } returns "Invalid Password"
    }

    context("Check if Password is correct") {
        given("a Password") {
            When("it's an invalid password") {
                then("Return an Invalid Password Error") {
                    viewModel.checkPassword("test")
                    viewModel.onValidPassword().value shouldBe Resource.error("Invalid Password")
                }
            }
            When("it's a valid password") {
                When("password is correct") {
                    then("return success") {
                        runTest {
                            viewModel.checkPassword("testValid")
                        }
                        viewModel.onValidPassword().value shouldBe Resource.success(Unit)
                    }
                }
                When("password is incorrect") {
                    then("return a failure") {
                        runTest {
                            viewModel.checkPassword("testInvalid")
                        }
                        viewModel.onValidPassword().value shouldBe
                            Resource.error("Invalid Password")
                    }
                }
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
        stopKoin()
    }
})
