package com.weatherxm.ui.signup

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
class SignupViewModelTest : BehaviorSpec({
    val usecase = mockk<AuthUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    val viewModel = SignupViewModel(usecase, resources, analytics)
    val username = "username"
    val invalidUsername = "Invalid Username"
    val userAlreadyExists = "User Already Exists"
    val invalidUsernameFailure = ApiError.AuthError.InvalidUsername("")
    val userAlreadyExistsFailure = ApiError.AuthError.SignupError.UserAlreadyExists("")
    val signupSuccess = "Signup Success"

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
            resources.getString(R.string.error_signup_invalid_username)
        } returns invalidUsername
        every {
            resources.getString(R.string.error_signup_user_already_exists, username)
        } returns userAlreadyExists
        every {
            resources.getString(R.string.success_signup_text, username)
        } returns signupSuccess
    }

    context("Signup") {
        given("A use case accepting a signup request") {
            When("it's a success") {
                coMockEitherRight({ usecase.signup(username, null, null) }, username)
                then("LiveData posts a success") {
                    runTest { viewModel.signup(username, null, null) }
                    viewModel.isSignedUp().isSuccess(signupSuccess)
                }
            }
            When("it's a failure") {
                and("It's an InvalidUsername failure") {
                    coMockEitherLeft(
                        { usecase.signup(username, null, null) },
                        invalidUsernameFailure
                    )
                    runTest { viewModel.signup(username, null, null) }
                    then("Log that error as a failure event") {
                        verify(exactly = 1) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData posts an error with a specific InvalidUsername message") {
                        viewModel.isSignedUp().isError(invalidUsername)
                    }
                }
                and("It's a UserAlreadyExists failure") {
                    coMockEitherLeft(
                        { usecase.signup(username, null, null) },
                        userAlreadyExistsFailure
                    )
                    runTest { viewModel.signup(username, null, null) }
                    then("Log that error as a failure event") {
                        verify(exactly = 2) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData posts an error with a specific UserAlreadyExists message") {
                        viewModel.isSignedUp().isError(userAlreadyExists)
                    }
                }
                and("it's any other failure") {
                    coMockEitherLeft({ usecase.signup(username, null, null) }, failure)
                    runTest { viewModel.signup(username, null, null) }
                    then("Log that error as a failure event") {
                        verify(exactly = 3) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData posts an error") {
                        viewModel.isSignedUp().isError(REACH_OUT_MSG)
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
