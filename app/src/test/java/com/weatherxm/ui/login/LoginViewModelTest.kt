package com.weatherxm.ui.login

import com.weatherxm.R
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.ApiError.AuthError.LoginError.InvalidCredentials
import com.weatherxm.data.models.ApiError.AuthError.LoginError.InvalidPassword
import com.weatherxm.data.models.User
import com.weatherxm.data.network.AuthToken
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
    lateinit var viewModel: LoginViewModel

    val authToken = mockk<AuthToken>()
    val username = "username"
    val password = "password"
    val invalidUsername = "Invalid Username"
    val invalidPassword = "Invalid Password"
    val invalidCredentials = "Invalid Credentials"
    val invalidUsernameFailure = ApiError.AuthError.InvalidUsername("")
    val invalidPasswordFailure = InvalidPassword("")
    val invalidCredentialsFailure = InvalidCredentials("")
    val user = User("id", username, null, null, null, null)

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
        justRun { analytics.setUserId(user.id) }
        coMockEitherRight({ usecase.isLoggedIn() }, true)
        every { usecase.shouldShowAnalyticsOptIn() } returns true
        every {
            resources.getString(R.string.error_login_invalid_username)
        } returns invalidUsername
        every {
            resources.getString(R.string.error_login_invalid_password)
        } returns invalidPassword
        every {
            resources.getString(R.string.error_login_invalid_credentials)
        } returns invalidCredentials

        viewModel = LoginViewModel(usecase, resources, analytics)
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

    context("Get if we should show the analytics opt-in screen or not") {
        given("A use case returning the result") {
            then("return that result") {
                viewModel.shouldShowAnalyticsOptIn() shouldBe true
            }
        }
    }

    context("A Login request") {
        given("A use case accepting that login request") {
            When("login is a failure") {
                and("It's an InvalidUsername failure") {
                    coMockEitherLeft({ usecase.login(username, password) }, invalidUsernameFailure)
                    testHandleFailureViewModel(
                        { viewModel.login(username, password) },
                        analytics,
                        viewModel.onLogin(),
                        1,
                        invalidUsername
                    )
                }
                and("It's an InvalidPassword failure") {
                    coMockEitherLeft({ usecase.login(username, password) }, invalidPasswordFailure)
                    testHandleFailureViewModel(
                        { viewModel.login(username, password) },
                        analytics,
                        viewModel.onLogin(),
                        2,
                        invalidPassword
                    )
                }
                and("It's an InvalidCredentials failure") {
                    coMockEitherLeft(
                        { usecase.login(username, password) },
                        invalidCredentialsFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.login(username, password) },
                        analytics,
                        viewModel.onLogin(),
                        3,
                        invalidCredentials
                    )
                }
                and("it's any other failure") {
                    coMockEitherLeft({ usecase.login(username, password) }, failure)
                    testHandleFailureViewModel(
                        { viewModel.login(username, password) },
                        analytics,
                        viewModel.onLogin(),
                        4,
                        REACH_OUT_MSG
                    )
                }
            }
            When("login is a success and fetching the user is a failure") {
                coMockEitherRight({ usecase.login(username, password) }, authToken)
                coMockEitherLeft({ usecase.getUser() }, failure)
                runTest { viewModel.login(username, password) }
                then("LiveData of login posts a success") {
                    viewModel.onLogin().isSuccess(Unit)
                }
                then("Log that error as a failure event") {
                    verify(exactly = 5) { analytics.trackEventFailure(any()) }
                }
                then("LiveData of user posts a failure") {
                    viewModel.user().isError(REACH_OUT_MSG)
                }
            }
            When("login is a success and fetching the user is a success also") {
                coMockEitherRight({ usecase.login(username, password) }, authToken)
                coMockEitherRight({ usecase.getUser() }, user)
                runTest { viewModel.login(username, password) }
                then("LiveData of login posts a success") {
                    viewModel.onLogin().isSuccess(Unit)
                }
                then("Set the user id in analytics") {
                    verify(exactly = 1) { analytics.setUserId(user.id) }
                }
                then("LiveData of user posts a success") {
                    viewModel.user().isSuccess(user)
                }
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
        stopKoin()
    }
})
