package com.weatherxm.ui.passwordprompt

import com.weatherxm.R
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
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
    val usecase = mockk<AuthUseCase>()
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
        justRun { analytics.trackEventFailure(any()) }
        every { failure.getDefaultMessage(R.string.error_invalid_password) } returns invalidPassMsg
        coMockEitherRight({ usecase.isPasswordCorrect(validPassword) }, true)
        coMockEitherLeft({ usecase.isPasswordCorrect(invalidPassword) }, failure)
    }

    context("Check if Password is correct") {
        given("a Password") {
            When("it's an invalid password (too small)") {
                then("Return an Invalid Password Error") {
                    viewModel.checkPassword(tooSmallPassword)
                    viewModel.onValidPassword().isError(invalidPassMsg)
                }
            }
            When("it's a valid password") {
                When("password is correct") {
                    then("return success") {
                        runTest { viewModel.checkPassword(validPassword) }
                        viewModel.onValidPassword().isSuccess(Unit)
                    }
                }
                When("password is incorrect") {
                    testHandleFailureViewModel(
                        { viewModel.checkPassword(invalidPassword) },
                        analytics,
                        viewModel.onValidPassword(),
                        1,
                        invalidPassMsg
                    )
                }
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
        stopKoin()
    }
})
