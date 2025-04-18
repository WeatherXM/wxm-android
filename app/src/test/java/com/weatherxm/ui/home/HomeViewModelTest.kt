package com.weatherxm.ui.home

import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.RemoteBanner
import com.weatherxm.data.models.RemoteBannerType
import com.weatherxm.data.models.Survey
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.WalletWarnings
import com.weatherxm.usecases.DevicePhotoUseCase
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
    val photosUseCase = mockk<DevicePhotoUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: HomeViewModel

    val walletMissing = WalletWarnings(showMissingBadge = true, showMissingWarning = true)
    val walletOK = WalletWarnings(showMissingBadge = false, showMissingWarning = false)
    val emptyWalletAddress = ""
    val survey = mockk<Survey>()
    val infoBanner = mockk<RemoteBanner>()
    val announcementBanner = mockk<RemoteBanner>()
    val surveyId = "surveyId"
    val bannerId = "bannerId"
    val deviceId = "deviceId"
    val devices = listOf(UIDevice.empty())

    listener(InstantExecutorListener())

    beforeSpec {
        justRun { analytics.trackEventFailure(any()) }
        justRun { userUseCase.setWalletWarningDismissTimestamp() }
        justRun { userUseCase.setAcceptTerms() }
        every { userUseCase.shouldShowWalletMissingWarning(emptyWalletAddress) } returns true
        every { userUseCase.shouldShowTermsPrompt() } returns true
        every { userUseCase.getClaimingBadgeShouldShow() } returns true
        every { remoteBannersUseCase.getSurvey() } returns survey
        justRun { remoteBannersUseCase.dismissSurvey(surveyId) }
        justRun { userUseCase.setClaimingBadgeShouldShow(any()) }
        every {
            remoteBannersUseCase.getRemoteBanner(RemoteBannerType.INFO_BANNER)
        } returns infoBanner
        every {
            remoteBannersUseCase.getRemoteBanner(RemoteBannerType.ANNOUNCEMENT)
        } returns announcementBanner
        justRun { remoteBannersUseCase.dismissRemoteBanner(RemoteBannerType.INFO_BANNER, bannerId) }
        justRun {
            remoteBannersUseCase.dismissRemoteBanner(RemoteBannerType.ANNOUNCEMENT, bannerId)
        }
        justRun { photosUseCase.retryUpload(deviceId) }

        viewModel =
            HomeViewModel(userUseCase, remoteBannersUseCase, photosUseCase, analytics, dispatcher)
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

    context("Flow to be called when scrolling takes place") {
        given("a dy value") {
            When("it's > 0") {
                viewModel.onScroll(1)
                then("LiveData showOverlayViews posts false") {
                    viewModel.showOverlayViews().value shouldBe false
                }
            }
            When("it's < 0") {
                viewModel.onScroll(-1)
                then("LiveData showOverlayViews posts true") {
                    viewModel.showOverlayViews().value shouldBe true
                }
            }
            When("it's = 0") {
                viewModel.onScroll(0)
                then("LiveData showOverlayViews posts true") {
                    viewModel.showOverlayViews().value shouldBe true
                }
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

    context("Get an RemoteBanner and then dismiss it") {
        given("an info banner ID") {
            and("get the RemoteBanner") {
                viewModel.getRemoteBanners()
                then("LiveData onInfoBanner should post that RemoteBanner value") {
                    viewModel.onInfoBanner().value shouldBe infoBanner
                }
                then("LiveData onAnnouncementBanner should post that RemoteBanner value") {
                    viewModel.onAnnouncementBanner().value shouldBe announcementBanner
                }
            }
            and("dismiss the info banner") {
                viewModel.dismissRemoteBanner(RemoteBannerType.INFO_BANNER, bannerId)
                then("the respective function in the usecase should be called") {
                    verify(exactly = 1) {
                        remoteBannersUseCase.dismissRemoteBanner(
                            RemoteBannerType.INFO_BANNER,
                            bannerId
                        )
                    }
                }
            }
            and("dismiss the announcement banner") {
                viewModel.dismissRemoteBanner(RemoteBannerType.ANNOUNCEMENT, bannerId)
                then("the respective function in the usecase should be called") {
                    verify(exactly = 1) {
                        remoteBannersUseCase.dismissRemoteBanner(
                            RemoteBannerType.ANNOUNCEMENT,
                            bannerId
                        )
                    }
                }
            }
        }
    }

    context("Get if we should show the terms prompt or not") {
        given("A use case returning the result") {
            then("return that result") {
                viewModel.shouldShowTerms.value shouldBe true
            }
        }
    }

    context("Set the Accept Terms") {
        given("the call to the respective function") {
            viewModel.setAcceptTerms()
            then("the respective function in the usecase should be called") {
                verify(exactly = 1) { userUseCase.setAcceptTerms() }
            }
        }
    }

    context("Retry photo uploading") {
        given("a deviceId") {
            then("trigger the retrying of photo uploading") {
                viewModel.retryPhotoUpload(deviceId)
                verify(exactly = 1) { photosUseCase.retryUpload(deviceId) }
            }
        }
    }

    context("Get if we should show the badge for the claiming or not") {
        given("A use case returning the result") {
            then("return that result") {
                viewModel.getClaimingBadgeShouldShow() shouldBe true
            }
        }
    }

    context("Set if we should show the badge for the claiming or not") {
        given("the call to the respective function") {
            viewModel.setClaimingBadgeShouldShow(false)
            then("the respective function in the usecase should be called") {
                verify(exactly = 1) { userUseCase.setClaimingBadgeShouldShow(false) }
            }
        }
    }
})
