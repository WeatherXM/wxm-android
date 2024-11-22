package com.weatherxm.ui.home

import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.InfoBanner
import com.weatherxm.data.models.Survey
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.WalletWarnings
import com.weatherxm.usecases.RemoteBannersUseCase
import com.weatherxm.usecases.UserUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest

class HomeViewModelTest : BehaviorSpec({
    val userUseCase = mockk<UserUseCase>()
    val remoteBannersUseCase = mockk<RemoteBannersUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: HomeViewModel

    val walletMissing = WalletWarnings(showMissingBadge = true, showMissingWarning = true)
    val walletOK = WalletWarnings(showMissingBadge = false, showMissingWarning = false)
    val emptyWalletAddress = ""
    val survey = mockk<Survey>()
    val infoBanner = mockk<InfoBanner>()
    val surveyId = "surveyId"
    val infoBannerId = "infoBannerId"
    val devices = listOf(UIDevice.empty())

    listener(InstantExecutorListener())

    beforeSpec {
        justRun { analytics.trackEventFailure(any()) }
        justRun { userUseCase.setWalletWarningDismissTimestamp() }
        every { userUseCase.shouldShowWalletMissingWarning(emptyWalletAddress) } returns true
        every { remoteBannersUseCase.getSurvey() } returns survey
        justRun { remoteBannersUseCase.dismissSurvey(surveyId) }
        every { remoteBannersUseCase.getInfoBanner() } returns infoBanner
        justRun { remoteBannersUseCase.dismissInfoBanner(infoBannerId) }

        viewModel = HomeViewModel(userUseCase, remoteBannersUseCase, analytics, dispatcher)
    }

    context("Flow when openExplorer is called") {
        given("the call") {
            viewModel.openExplorer()
            then("LiveData onOpenExplorer should post the value true") {
                viewModel.onOpenExplorer().value shouldBe true
            }
        }
    }

    context("GET and SET the hasDevices property") {
        When("GET the hasDevices") {
            then("get the initial null value") {
                viewModel.hasDevices() shouldBe null
            }
        }
        When("SET the hasDevices") {
            then("call the respective function") {
                viewModel.setHasDevices(devices)
            }
            then("GET the property again") {
                viewModel.hasDevices() shouldBe false
            }
        }
    }

    context("Get Wallet Warnings") {
        given("a usecase returning the wallet address in order to create the WalletWarnings") {
            When("it's a failure") {
                coMockEitherLeft({ userUseCase.getWalletAddress() }, failure)
                runTest { viewModel.getWalletWarnings() }
                then("LiveData onWalletWarnings should post the respective WalletWarnings value") {
                    viewModel.onWalletWarnings().value shouldBe walletOK
                }
                then("track event's failure in analytics") {
                    verify(exactly = 1) { analytics.trackEventFailure(any()) }
                }
            }
            When("it's a success") {
                coMockEitherRight({ userUseCase.getWalletAddress() }, emptyWalletAddress)
                runTest { viewModel.getWalletWarnings() }
                then("LiveData onWalletWarnings should post the respective WalletWarnings value") {
                    viewModel.onWalletWarnings().value shouldBe walletMissing
                }
            }
        }
    }

    context("Set the timestamp of the dismissal of the wallet warning") {
        given("the call to the respective function") {
            viewModel.setWalletWarningDismissTimestamp()
            then("the respective function in the usecase should be called") {
                verify(exactly = 1) { userUseCase.setWalletWarningDismissTimestamp() }
            }
        }
    }

    context("Set the wallet as not missing") {
        given("the call to the respective function") {
            viewModel.setWalletNotMissing()
            then("LiveData onWalletWarnings should post the respective WalletWarnings value") {
                viewModel.onWalletWarnings().value shouldBe walletOK
            }
        }
    }

    context("Get a Survey and then dismiss it") {
        given("a surveyId") {
            and("get the survey") {
                viewModel.getSurvey()
                then("LiveData onSurvey should post that survey value") {
                    viewModel.onSurvey().value shouldBe survey
                }
            }
            and("dismiss it") {
                viewModel.dismissSurvey(surveyId)
                then("the respective function in the usecase should be called") {
                    verify(exactly = 1) { remoteBannersUseCase.dismissSurvey(surveyId) }
                }
            }
        }
    }

    context("Get an InfoBanner and then dismiss it") {
        given("an info banner ID") {
            and("get the InfoBanner") {
                viewModel.getInfoBanner()
                then("LiveData onInfoBanner should post that InfoBanner value") {
                    viewModel.onInfoBanner().value shouldBe infoBanner
                }
            }
            and("dismiss it") {
                viewModel.dismissInfoBanner(infoBannerId)
                then("the respective function in the usecase should be called") {
                    verify(exactly = 1) { remoteBannersUseCase.dismissInfoBanner(infoBannerId) }
                }
            }
        }
    }
})
