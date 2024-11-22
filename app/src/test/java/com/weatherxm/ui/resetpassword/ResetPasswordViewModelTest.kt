package com.weatherxm.ui.resetpassword

import com.weatherxm.R
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ResetPasswordViewModelTest : BehaviorSpec({
    val usecase = mockk<AuthUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    val viewModel = ResetPasswordViewModel(usecase, resources, analytics, dispatcher)
    val email = "email"
    val invalidUsername = "Invalid Username"
    val invalidUsernameFailure = ApiError.AuthError.InvalidUsername("")

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
        justRun { analytics.trackEventFailure(any()) }
        every {
            resources.getString(R.string.error_password_reset_invalid_username)
        } returns invalidUsername
    }

    context("Reset Password") {
        given("A use case accepting a reset password request") {
            When("it's a success") {
                coMockEitherRight({ usecase.resetPassword(email) }, Unit)
                then("LiveData posts a success") {
                    runTest { viewModel.resetPassword(email) }
                    viewModel.isEmailSent().isSuccess(Unit)
                }
            }
            When("it's a failure") {
                and("It's an InvalidUsername failure") {
                    coMockEitherLeft(
                        { usecase.resetPassword(email) },
                        invalidUsernameFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.resetPassword(email) },
                        analytics,
                        viewModel.isEmailSent(),
                        1,
                        invalidUsername
                    )
                }
                and("it's any other failure") {
                    coMockEitherLeft({ usecase.resetPassword(email) }, failure)
                    testHandleFailureViewModel(
                        { viewModel.resetPassword(email) },
                        analytics,
                        viewModel.isEmailSent(),
                        2,
                        REACH_OUT_MSG
                    )
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
