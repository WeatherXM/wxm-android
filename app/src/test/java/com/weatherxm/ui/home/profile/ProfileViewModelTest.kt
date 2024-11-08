package com.weatherxm.ui.home.profile

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
import com.weatherxm.data.models.User
import com.weatherxm.data.models.Wallet
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.UIWalletRewards
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ProfileViewModelTest : BehaviorSpec({
    val userUseCase = mockk<UserUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: ProfileViewModel

    val walletAddress = "walletAddress"
    val walletRewards = UIWalletRewards(100.0, 0.0, 100.0, walletAddress)
    val walletRewardsAfterClaim = UIWalletRewards(100.0, 10.0, 90.0, walletAddress)
    val amountClaimed = 10.0
    val user = User("id", "email", null, null, null, Wallet(walletAddress, null))
    val walletAddressNotFoundFailure = ApiError.UserError.WalletError.WalletAddressNotFound("")

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
        viewModel = ProfileViewModel(userUseCase, analytics, dispatcher)
    }

    context("Get the User") {
        given("a usecase returning the User") {
            When("it's a failure") {
                coMockEitherLeft({ userUseCase.getUser(false) }, failure)
                testHandleFailureViewModel(
                    { viewModel.fetchUser() },
                    analytics,
                    viewModel.onUser(),
                    1,
                    REACH_OUT_MSG
                )
            }
            When("it's a success") {
                coMockEitherRight({ userUseCase.getUser(true) }, user)
                and("fetching wallet rewards is a failure") {
                    and("it's a WalletAddressNotFound failure") {
                        coMockEitherLeft(
                            { userUseCase.getWalletRewards(walletAddress) },
                            walletAddressNotFoundFailure
                        )
                        runTest { viewModel.fetchUser(true) }
                        then("LiveData onWalletRewards should post the an empty rewards object") {
                            viewModel.onWalletRewards().isSuccess(UIWalletRewards.empty())
                        }
                    }
                    and("it's any other failure") {
                        coMockEitherLeft(
                            { userUseCase.getWalletRewards(walletAddress) },
                            failure
                        )
                        testHandleFailureViewModel(
                            { viewModel.fetchUser(true) },
                            analytics,
                            viewModel.onWalletRewards(),
                            3,
                            REACH_OUT_MSG
                        )
                    }
                }
                and("fetching wallet rewards is a success") {
                    coMockEitherRight(
                        { userUseCase.getWalletRewards(walletAddress) },
                        walletRewards
                    )
                    runTest { viewModel.fetchUser(true) }
                    then("LiveData onWalletRewards should post the latest rewards") {
                        viewModel.onWalletRewards().isSuccess(walletRewards)
                    }
                }
                then("LiveData onUser should post the User value") {
                    viewModel.onUser().isSuccess(user)
                }
            }
        }
    }

    context("Flow when a claim was successful and we need to update the saved rewards") {
        given("the amount that has been claimed") {
            viewModel.onClaimedResult(amountClaimed)
            then("Some math should take place and onWalletRewards should post the new rewards") {
                viewModel.onWalletRewards().isSuccess(walletRewardsAfterClaim)
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
