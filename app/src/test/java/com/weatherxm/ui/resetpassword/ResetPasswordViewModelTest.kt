package com.weatherxm.ui.resetpassword

import com.weatherxm.R
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
class ResetPasswordViewModelTest : BehaviorSpec({
    val usecase = mockk<AuthUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    val viewModel = ResetPasswordViewModel(usecase, resources, analytics)
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
                    runTest { viewModel.resetPassword(email) }
                    then("Log that error as a failure event") {
                        verify(exactly = 1) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData posts an error with a specific InvalidUsername message") {
                        viewModel.isEmailSent().isError(invalidUsername)
                    }
                }
                and("it's any other failure") {
                    coMockEitherLeft({ usecase.resetPassword(email) }, failure)
                    runTest { viewModel.resetPassword(email) }
                    then("Log that error as a failure event") {
                        verify(exactly = 2) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData posts an error") {
                        viewModel.isEmailSent().isError(REACH_OUT_MSG)
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
