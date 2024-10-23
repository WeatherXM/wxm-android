package com.weatherxm.ui.deleteaccount

import com.weatherxm.R
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError.AuthError.LoginError.InvalidCredentials
import com.weatherxm.data.models.ApiError.AuthError.LoginError.InvalidPassword
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.deleteaccount.DeleteAccountViewModel.Companion.DELETE_ACCOUNT_DELAY
import com.weatherxm.usecases.DeleteAccountUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAccountViewModelTest : BehaviorSpec({
    val usecase = mockk<DeleteAccountUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    val viewModel = DeleteAccountViewModel(usecase, resources, analytics)

    val invalidPasswordMsg = "Invalid Password"
    val password = "password"
    val emptyPassword = String.empty()
    val invalidCredentialsFailure = InvalidCredentials("")

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
        justRun { analytics.onLogout() }
        every { resources.getString(R.string.warn_invalid_password) } returns invalidPasswordMsg
    }

    context("Delete a user account") {
        given("a password") {
            When("the password is invalid") {
                then("LiveData at onStatus posts an error") {
                    runTest { viewModel.checkAndDeleteAccount(emptyPassword) }
                    viewModel.onStatus().value?.apply {
                        message shouldBe invalidPasswordMsg
                        status shouldBe com.weatherxm.ui.common.Status.ERROR
                        data?.status shouldBe Status.PASSWORD_VERIFICATION
                        data?.failure?.shouldBeTypeOf<InvalidPassword>()
                    }
                }
            }
            When("the password is valid") {
                and("checking if the password is correct fails") {
                    When("it's an InvalidCredentials failure") {
                        coMockEitherLeft(
                            { usecase.isPasswordCorrect(password) },
                            invalidCredentialsFailure
                        )
                        then("LiveData at onStatus posts an error") {
                            runTest { viewModel.checkAndDeleteAccount(password) }
                            /**
                             * Use a longer delay than the one in the checkAndDeleteAccount fun
                             * in order to wait before the values are set:
                             * https://stackoverflow.com/questions/53271646/how-to-unit-test-coroutine-when-it-contains-coroutine-delay
                             */
                            delay(DELETE_ACCOUNT_DELAY + DELETE_ACCOUNT_DELAY)
                            viewModel.onStatus().value?.apply {
                                message shouldBe invalidPasswordMsg
                                status shouldBe com.weatherxm.ui.common.Status.ERROR
                                data?.status shouldBe Status.PASSWORD_VERIFICATION
                                data?.failure?.shouldBeTypeOf<InvalidPassword>()
                            }
                        }
                        then("track failure event in analytics") {
                            verify(exactly = 1) { analytics.trackEventFailure(any()) }
                        }
                    }
                    When("it's any other failure") {
                        coMockEitherLeft({ usecase.isPasswordCorrect(password) }, failure)
                        then("LiveData at onStatus posts an error") {
                            runTest { viewModel.checkAndDeleteAccount(password) }
                            delay(DELETE_ACCOUNT_DELAY + DELETE_ACCOUNT_DELAY)
                            viewModel.onStatus().value?.apply {
                                message shouldBe REACH_OUT_MSG
                                status shouldBe com.weatherxm.ui.common.Status.ERROR
                                data?.status shouldBe Status.PASSWORD_VERIFICATION
                                data?.failure shouldBe null
                            }
                        }
                        then("track failure event in analytics") {
                            verify(exactly = 2) { analytics.trackEventFailure(any()) }
                        }
                    }
                }
                and("the password is correct") {
                    coMockEitherRight({ usecase.isPasswordCorrect(password) }, true)
                    and("delete the account") {
                        When("it fails") {
                            coMockEitherLeft({ usecase.deleteAccount() }, failure)
                            then("LiveData at onStatus posts an error") {
                                runTest { viewModel.checkAndDeleteAccount(password) }
                                delay(DELETE_ACCOUNT_DELAY + DELETE_ACCOUNT_DELAY)
                                viewModel.onStatus().value?.apply {
                                    message shouldBe failure.code
                                    status shouldBe com.weatherxm.ui.common.Status.ERROR
                                    data?.status shouldBe Status.ACCOUNT_DELETION
                                    data?.failure shouldBe null
                                }
                            }
                            then("track failure event in analytics") {
                                verify(exactly = 3) { analytics.trackEventFailure(any()) }
                            }
                        }
                        When("it succeeds") {
                            coMockEitherRight({ usecase.deleteAccount() }, Unit)
                            then("LiveData at onStatus posts a success") {
                                runTest { viewModel.checkAndDeleteAccount(password) }
                                delay(DELETE_ACCOUNT_DELAY + DELETE_ACCOUNT_DELAY)
                                viewModel.onStatus().value?.apply {
                                    this.status shouldBe com.weatherxm.ui.common.Status.SUCCESS
                                    data?.status shouldBe Status.ACCOUNT_DELETION
                                }
                            }
                            then("call logout in Analytics") {
                                verify(exactly = 1) { analytics.onLogout() }
                            }
                            then("get if account is deleted") {
                                viewModel.isAccountedDeleted() shouldBe true
                            }
                            then("get if we are in a safe state") {
                                viewModel.isOnSafeState() shouldBe true
                            }
                        }
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
