package com.weatherxm.ui.passwordprompt

import arrow.core.Either
import com.weatherxm.R
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.ApiError
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.usecases.PasswordPromptUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
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
    val tooSmallPassword = "test"
    val validPassword = "testValid"
    val invalidPassword = "testInvalid"
    val invalidPassMsg = "Invalid Password"

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

        val failure = ApiError.AuthError.LoginError.InvalidPassword("")
        every { resources.getString(R.string.error_invalid_password) } returns invalidPassMsg
        every { analytics.trackEventFailure(any()) } just Runs
        every { failure.getDefaultMessage(R.string.error_invalid_password) } returns invalidPassMsg
        coEvery { usecase.isPasswordCorrect(validPassword) } returns Either.Right(Unit)
        coEvery { usecase.isPasswordCorrect(invalidPassword) } returns Either.Left(failure)
    }

    context("Check if Password is correct") {
        given("a Password") {
            When("it's an invalid password (too small)") {
                then("Return an Invalid Password Error") {
                    viewModel.checkPassword(tooSmallPassword)
                    viewModel.onValidPassword().value?.isError(invalidPassMsg)
                }
            }
            When("it's a valid password") {
                When("password is correct") {
                    then("return success") {
                        runTest { viewModel.checkPassword(validPassword) }
                        viewModel.onValidPassword().value?.isSuccess(Unit)
                    }
                }
                When("password is incorrect") {
                    then("return a failure") {
                        runTest { viewModel.checkPassword(invalidPassword) }
                        viewModel.onValidPassword().value?.isError(invalidPassMsg)
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
