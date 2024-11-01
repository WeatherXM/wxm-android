package com.weatherxm.ui.claimdevice.pulse

import com.weatherxm.R
import com.weatherxm.TestConfig.DEVICE_NOT_FOUND_MSG
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.Location
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
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
class ClaimPulseViewModelTest : BehaviorSpec({
    val usecase = mockk<ClaimDeviceUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: ClaimPulseViewModel

    val serial = "serialNumber"
    val claimingKey = "claimingKey"
    val validSerialNumber = "0123456789ABCDEF"
    val validClaimingKey = "012345"
    val location = Location(0.0, 0.0)

    val invalidSerial = "Invalid Serial"
    val invalidLocation = "Invalid Location"
    val alreadyClaimed = "Already Claimed"
    val deviceClaimingError = "Device Claiming Error"

    val device = UIDevice.empty()

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

        every { resources.getString(R.string.error_claim_invalid_serial) } returns invalidSerial
        every { resources.getString(R.string.error_claim_invalid_location) } returns invalidLocation
        every { resources.getString(R.string.error_claim_already_claimed) } returns alreadyClaimed
        every { resources.getString(R.string.error_claim_not_found) } returns DEVICE_NOT_FOUND_MSG
        every {
            resources.getString(R.string.error_claim_device_claiming_error)
        } returns deviceClaimingError

        viewModel = ClaimPulseViewModel(usecase, resources, analytics)
    }


    suspend fun BehaviorSpecWhenContainerScope.testClaimingFailure(
        verifyNumberOfFailureEvents: Int,
        errorMsg: String
    ) {
        testHandleFailureViewModel(
            { viewModel.claimDevice(location) },
            analytics,
            viewModel.onClaimResult(),
            verifyNumberOfFailureEvents,
            errorMsg
        )
    }

    context("Perform next and cancel actions in the claiming flow") {
        given("a `next` request") {
            and("the `pages` we should move forward") {
                When("no arg for `pages` is being given") {
                    then("use the default (1) and LiveData onNext posts it") {
                        viewModel.next()
                        viewModel.onNext().value shouldBe 1
                    }
                }
                When("specific pages to move forward is given") {
                    then("LiveData onNext posts it") {
                        viewModel.next(2)
                        viewModel.onNext().value shouldBe 2
                    }
                }
            }
        }
        given("a `cancel` request") {
            then("LiveData onCancel posts a true value") {
                viewModel.onCancel().value shouldBe false
                viewModel.cancel()
                viewModel.onCancel().value shouldBe true
            }
        }
    }

    context("SET a serial number and then GET it") {
        given("a serial number") {
            then("ensure that the current one is empty") {
                viewModel.getSerialNumber() shouldBe String.empty()
            }
            and("SET it") {
                viewModel.setSerialNumber(serial)
                then("GET it and ensure it's set correctly") {
                    viewModel.getSerialNumber() shouldBe serial
                }
            }
        }
    }

    context("SET a claiming key and then GET it") {
        given("a claiming key") {
            then("ensure that the current one is null") {
                viewModel.getClaimingKey() shouldBe null
            }
            and("SET it") {
                viewModel.setClaimingKey(claimingKey)
                then("GET it and ensure it's set correctly") {
                    viewModel.getClaimingKey() shouldBe claimingKey
                }
            }
        }
    }

    context("Validate serial number and claiming key") {
        given("A serial number") {
            When("it's invalid") {
                then("return false") {
                    viewModel.validateSerial(serial) shouldBe false
                }
            }
            When("it's valid") {
                then("return true") {
                    viewModel.validateSerial(validSerialNumber) shouldBe true
                }
            }
        }
        given("A claiming key") {
            When("it's invalid") {
                then("return false") {
                    viewModel.validateClaimingKey(claimingKey) shouldBe false
                }
            }
            When("it's valid") {
                then("return true") {
                    viewModel.validateClaimingKey(validClaimingKey) shouldBe true
                }
            }
        }
    }

    context("Claim a Wifi device") {
        given("a usecase providing the result of the claiming") {
            When("it's a failure") {
                and("It's an InvalidClaimId failure") {
                    coMockEitherLeft(
                        { usecase.claimDevice(serial, location.lat, location.lon, claimingKey) },
                        ApiError.UserError.ClaimError.InvalidClaimId("")
                    )
                    testClaimingFailure(1, invalidSerial)
                }
                and("It's an InvalidClaimLocation failure") {
                    coMockEitherLeft(
                        { usecase.claimDevice(serial, location.lat, location.lon, claimingKey) },
                        ApiError.UserError.ClaimError.InvalidClaimLocation("")
                    )
                    testClaimingFailure(2, invalidLocation)
                }
                and("It's an DeviceAlreadyClaimed failure") {
                    coMockEitherLeft(
                        { usecase.claimDevice(serial, location.lat, location.lon, claimingKey) },
                        ApiError.UserError.ClaimError.DeviceAlreadyClaimed("")
                    )
                    testClaimingFailure(3, alreadyClaimed)
                }
                and("It's an DeviceNotFound failure") {
                    coMockEitherLeft(
                        { usecase.claimDevice(serial, location.lat, location.lon, claimingKey) },
                        ApiError.DeviceNotFound("")
                    )
                    testClaimingFailure(4, DEVICE_NOT_FOUND_MSG)
                }
                and("It's an DeviceClaiming failure") {
                    coMockEitherLeft(
                        { usecase.claimDevice(serial, location.lat, location.lon, claimingKey) },
                        ApiError.UserError.ClaimError.DeviceClaiming("")
                    )
                    testClaimingFailure(5, deviceClaimingError)
                }
                and("it's any other failure") {
                    coMockEitherLeft(
                        { usecase.claimDevice(serial, location.lat, location.lon, claimingKey) },
                        failure
                    )
                    testClaimingFailure(6, REACH_OUT_MSG)
                }
            }
            When("it's a success") {
                coMockEitherRight(
                    { usecase.claimDevice(serial, location.lat, location.lon, claimingKey) },
                    device
                )
                runTest { viewModel.claimDevice(location) }
                then("LiveData onClaimResult posts a success with the device") {
                    viewModel.onClaimResult().isSuccess(device)
                }
            }
        }

    }

    afterSpec {
        Dispatchers.resetMain()
        stopKoin()
    }
})
