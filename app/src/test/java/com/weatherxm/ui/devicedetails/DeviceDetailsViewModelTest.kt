package com.weatherxm.ui.devicedetails

import com.weatherxm.R
import com.weatherxm.TestConfig.DEVICE_NOT_FOUND_MSG
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.DeviceDetailsUseCase
import com.weatherxm.usecases.FollowUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.time.Duration.Companion.parse

@OptIn(FlowPreview::class)
class DeviceDetailsViewModelTest : BehaviorSpec({
    val deviceDetailsUseCase = mockk<DeviceDetailsUseCase>()
    val authUseCase = mockk<AuthUseCase>()
    val followUseCase = mockk<FollowUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: DeviceDetailsViewModel

    val emptyDevice = UIDevice.empty()
    val device = UIDevice(
        "deviceId",
        "My Weather Station",
        String.empty(),
        DeviceRelation.OWNED,
        null,
        "friendlyName",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        100F,
        100F,
        null,
        null,
        null
    )
    val deviceNotFoundFailure = ApiError.DeviceNotFound("")
    val maxFollowedFailure = ApiError.MaxFollowed("")
    val unauthorizedFailure = ApiError.GenericError.JWTError.UnauthorizedError("", "unauthorized")
    val maxFollowedMsg = "Max Followed Failure"

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
        justRun {
            analytics.trackEventUserAction(
                AnalyticsService.ParamValue.DEVICE_DETAILS_FOLLOW.paramValue,
                AnalyticsService.ParamValue.UNFOLLOW.paramValue
            )
        }
        justRun {
            analytics.trackEventUserAction(
                AnalyticsService.ParamValue.DEVICE_DETAILS_FOLLOW.paramValue,
                AnalyticsService.ParamValue.FOLLOW.paramValue
            )
        }
        every { authUseCase.isLoggedIn() } returns true
        every { resources.getString(R.string.error_max_followed) } returns maxFollowedMsg
        justRun { deviceDetailsUseCase.setAcceptTerms() }
        every { deviceDetailsUseCase.shouldShowTermsPrompt() } returns true

        viewModel = DeviceDetailsViewModel(
            emptyDevice,
            false,
            deviceDetailsUseCase,
            authUseCase,
            followUseCase,
            resources,
            analytics,
            dispatcher
        )
    }

    context("Get if the user is logged in already or not") {
        given("A use case returning the result") {
            When("it's a success") {
                then("LiveData posts a success") {
                    runTest { viewModel.isLoggedIn() }
                    viewModel.isLoggedIn() shouldBe true
                }
                then("get if we should show a failure or not") {
                    viewModel.onShowLegalTerms().value shouldBe true
                }
            }
        }
    }

    context("GET / SET the device") {
        When("GET the device") {
            then("return the default empty device") {
                viewModel.device shouldBe emptyDevice
            }
        }
        When("SET a new device") {
            viewModel.updateDevice(device)
            then("GET it to ensure it has been set") {
                viewModel.device shouldBe device
            }
            then("LiveData onUpdatedDevice should post that device") {
                viewModel.onUpdatedDevice().value shouldBe device
            }
        }
    }

    context("Follow a station") {
        given("a usecase returning the response of the unfollow request") {
            When("it's failure") {
                and("it's a DeviceNotFound failure") {
                    coMockEitherLeft(
                        { followUseCase.followStation(device.id) },
                        deviceNotFoundFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.followStation() },
                        analytics,
                        viewModel.onFollowStatus(),
                        1,
                        DEVICE_NOT_FOUND_MSG
                    )
                }
                and("it's a MaxFollowed failure") {
                    coMockEitherLeft(
                        { followUseCase.followStation(device.id) },
                        maxFollowedFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.followStation() },
                        analytics,
                        viewModel.onFollowStatus(),
                        2,
                        maxFollowedMsg
                    )
                }
                and("it's a UnauthorizedError failure") {
                    coMockEitherLeft(
                        { followUseCase.followStation(device.id) },
                        unauthorizedFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.followStation() },
                        analytics,
                        viewModel.onFollowStatus(),
                        3,
                        unauthorizedFailure.message ?: String.empty()
                    )
                }
                and("it's any other failure") {
                    coMockEitherLeft(
                        { followUseCase.followStation(device.id) },
                        failure
                    )
                    testHandleFailureViewModel(
                        { viewModel.followStation() },
                        analytics,
                        viewModel.onFollowStatus(),
                        4,
                        REACH_OUT_MSG
                    )
                }
            }
            When("it's a success") {
                coMockEitherRight({ followUseCase.followStation(device.id) }, Unit)
                runTest { viewModel.followStation() }
                then("LiveData onFollowStatus should post the value Unit as success") {
                    viewModel.onFollowStatus().isSuccess(Unit)
                }
                then("update the device's relation to FOLLOWED") {
                    viewModel.device.relation shouldBe DeviceRelation.FOLLOWED
                }
            }
            then("Track the event UNFOLLOW as many times as the function calls") {
                verify(exactly = 5) {
                    analytics.trackEventUserAction(
                        AnalyticsService.ParamValue.DEVICE_DETAILS_FOLLOW.paramValue,
                        AnalyticsService.ParamValue.FOLLOW.paramValue
                    )
                }
            }
        }
    }

    context("Unfollow a station") {
        given("a usecase returning the response of the unfollow request") {
            When("it's failure") {
                and("it's a DeviceNotFound failure") {
                    coMockEitherLeft(
                        { followUseCase.unfollowStation(device.id) },
                        deviceNotFoundFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.unFollowStation() },
                        analytics,
                        viewModel.onFollowStatus(),
                        5,
                        DEVICE_NOT_FOUND_MSG
                    )
                }
                and("it's a MaxFollowed failure") {
                    coMockEitherLeft(
                        { followUseCase.unfollowStation(device.id) },
                        maxFollowedFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.unFollowStation() },
                        analytics,
                        viewModel.onFollowStatus(),
                        6,
                        maxFollowedMsg
                    )
                }
                and("it's a UnauthorizedError failure") {
                    coMockEitherLeft(
                        { followUseCase.unfollowStation(device.id) },
                        unauthorizedFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.unFollowStation() },
                        analytics,
                        viewModel.onFollowStatus(),
                        7,
                        unauthorizedFailure.message ?: String.empty()
                    )
                }
                and("it's any other failure") {
                    coMockEitherLeft(
                        { followUseCase.unfollowStation(device.id) },
                        failure
                    )
                    testHandleFailureViewModel(
                        { viewModel.unFollowStation() },
                        analytics,
                        viewModel.onFollowStatus(),
                        8,
                        REACH_OUT_MSG
                    )
                }
            }
            When("it's a success") {
                coMockEitherRight({ followUseCase.unfollowStation(device.id) }, Unit)
                runTest { viewModel.unFollowStation() }
                then("LiveData onFollowStatus should post the value Unit as success") {
                    viewModel.onFollowStatus().isSuccess(Unit)
                }
                then("update the device's relation to UNFOLLOWED") {
                    viewModel.device.relation shouldBe DeviceRelation.UNFOLLOWED
                }
            }
            then("Track the event UNFOLLOW as many times as the function calls") {
                verify(exactly = 5) {
                    analytics.trackEventUserAction(
                        AnalyticsService.ParamValue.DEVICE_DETAILS_FOLLOW.paramValue,
                        AnalyticsService.ParamValue.UNFOLLOW.paramValue
                    )
                }
            }
        }
    }

    @Suppress("SwallowedException")
    context("Use the automated device fetch") {
        given("a usecase returning the response of the fetch request") {
            When("it's failure") {
                coMockEitherLeft({ deviceDetailsUseCase.getDevice(device) }, failure)
                try {
                    viewModel.deviceAutoRefresh().timeout(parse("1s")).collectLatest {
                        it.isError()
                        verify(exactly = 9) { analytics.trackEventFailure(any()) }
                    }
                } catch (e: TimeoutCancellationException) {
                    /**
                     * Do nothing.
                     * We use timeout to terminate the flow collection (and the test)
                     */
                }
            }
            When("it's a success") {
                and("the current device is NOT empty") {
                    viewModel.updateDevice(device)
                    coMockEitherRight({ deviceDetailsUseCase.getDevice(device) }, device)
                    then("The device should be updated and LiveData(s) should post the values") {
                        try {
                            viewModel.deviceAutoRefresh().timeout(parse("1s")).collectLatest {
                                viewModel.onDeviceFirstFetch().value shouldBe null
                                viewModel.device shouldBe device
                                viewModel.onDevicePolling().value shouldBe device
                            }
                        } catch (e: TimeoutCancellationException) {
                            /**
                             * Do nothing.
                             * We use timeout to terminate the flow collection (and the test)
                             */
                        }
                    }
                }
                and("the current device is empty") {
                    viewModel.updateDevice(emptyDevice)
                    coMockEitherRight({ deviceDetailsUseCase.getDevice(emptyDevice) }, device)
                    then("The device should be updated and LiveData(s) should post the values") {
                        try {
                            viewModel.deviceAutoRefresh().timeout(parse("1s")).collectLatest {
                                viewModel.onDeviceFirstFetch().value shouldBe device
                                viewModel.device shouldBe device
                                viewModel.onDevicePolling().value shouldBe device
                            }
                        } catch (e: TimeoutCancellationException) {
                            /**
                             * Do nothing.
                             * We use timeout to terminate the flow collection (and the test)
                             */
                        }
                    }
                }
            }
        }
    }

    context("Set the Accept Terms") {
        given("the call to the respective function") {
            viewModel.setAcceptTerms()
            then("the respective function in the usecase should be called") {
                verify(exactly = 1) { deviceDetailsUseCase.setAcceptTerms() }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
