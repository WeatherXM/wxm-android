package com.weatherxm.ui.claimdevice.helium

import com.weatherxm.R
import com.weatherxm.TestConfig.DEVICE_NOT_FOUND_MSG
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.Frequency
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.PhotoPresignedMetadata
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.usecases.DevicePhotoUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ClaimHeliumViewModelTest : BehaviorSpec({
    val usecase = mockk<ClaimDeviceUseCase>()
    val photoUseCase = mockk<DevicePhotoUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: ClaimHeliumViewModel

    val devEUI = "devEUI"
    val deviceKey = "deviceKey"
    val frequency = Frequency.EU868
    val location = Location(0.0, 0.0)

    val invalidClaimIdFailure = ApiError.UserError.ClaimError.InvalidClaimId("")
    val invalidClaimLocationFailure = ApiError.UserError.ClaimError.InvalidClaimLocation("")
    val deviceAlreadyClaimedFailure = ApiError.UserError.ClaimError.DeviceAlreadyClaimed("")
    val deviceNotFoundFailure = ApiError.DeviceNotFound("")
    val deviceClaimingFailure = ApiError.UserError.ClaimError.DeviceClaiming("")
    val invalidEUI = "Invalid Dev EUI"
    val invalidLocation = "Invalid Location"
    val alreadyClaimed = "Already Claimed"
    val deviceClaimingError = "Device Claiming Error"
    val stationPhotos = listOf(
        StationPhoto("remotePath", "localPath")
    )
    val photoMetadata = listOf<PhotoPresignedMetadata>()

    val device = UIDevice.empty()

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

        every { resources.getString(R.string.error_claim_invalid_dev_eui) } returns invalidEUI
        every { resources.getString(R.string.error_claim_invalid_location) } returns invalidLocation
        every { resources.getString(R.string.error_claim_already_claimed) } returns alreadyClaimed
        every {
            resources.getString(R.string.error_claim_not_found_helium)
        } returns DEVICE_NOT_FOUND_MSG
        every {
            resources.getString(R.string.error_claim_device_claiming_error)
        } returns deviceClaimingError
        coMockEitherRight(
            { photoUseCase.getPhotosMetadataForUpload(device.id, listOf("localPath")) },
            photoMetadata
        )

        viewModel = ClaimHeliumViewModel(usecase, photoUseCase, resources, analytics, dispatcher)
    }


    suspend fun BehaviorSpecWhenContainerScope.testClaimingFailure(
        verifyNumberOfFailureEvents: Int,
        errorMsg: String
    ) {
        runTest { viewModel.claimDevice(location, stationPhotos) }
        then("Log that error as a failure event") {
            verify(exactly = verifyNumberOfFailureEvents) { analytics.trackEventFailure(any()) }
        }
        then("LiveData posts an error with a specific $errorMsg message") {
            val claimResult = viewModel.onClaimResult().value
            claimResult?.status shouldBe Status.ERROR
            claimResult?.message shouldBe errorMsg
        }
    }

    context("Perform next and cancel actions in the claiming flow") {
        given("a `next` request") {
            then("LiveData onNext posts a true value") {
                viewModel.onNext().value shouldBe false
                viewModel.next()
                viewModel.onNext().value shouldBe true
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

    context("SET a Dev EUI and then GET it") {
        given("a Dev EUI") {
            then("ensure that the current one is empty") {
                viewModel.getDevEUI() shouldBe String.empty()
            }
            and("SET it") {
                viewModel.setDeviceEUI(devEUI)
                then("GET it and ensure it's set correctly") {
                    viewModel.getDevEUI() shouldBe devEUI
                }
            }
        }
    }

    context("SET a device key and then GET it") {
        given("a device key") {
            then("ensure that the current one is empty") {
                viewModel.getDeviceKey() shouldBe String.empty()
            }
            and("SET it") {
                viewModel.setDeviceKey(deviceKey)
                then("GET it and ensure it's set correctly") {
                    viewModel.getDeviceKey() shouldBe deviceKey
                }
            }
        }
    }

    context("SET a Frequency and then GET it") {
        given("a Frequency") {
            then("ensure that the current one is the default one") {
                viewModel.getFrequency() shouldBe Frequency.US915
            }
            and("SET it") {
                viewModel.setFrequency(frequency)
                then("GET it and ensure it's set correctly") {
                    viewModel.getFrequency() shouldBe frequency
                }
            }
        }
    }

    context("Claim a Helium device") {
        given("a usecase providing the result of the claiming") {
            When("it's a success") {
                coMockEitherRight(
                    { usecase.claimDevice(devEUI, location.lat, location.lon, deviceKey) },
                    device
                )
                runTest { viewModel.claimDevice(location, stationPhotos) }
                then("Get the photos metadata to upload") {
                    viewModel.onPhotosMetadata().value shouldBe Pair(device, photoMetadata)
                }
                then("LiveData onClaimResult posts a success with the device") {
                    viewModel.onClaimResult().isSuccess(device)
                }
            }
            When("it's a failure") {
                and("It's an InvalidClaimId failure") {
                    coMockEitherLeft(
                        { usecase.claimDevice(devEUI, location.lat, location.lon, deviceKey) },
                        invalidClaimIdFailure
                    )
                    testClaimingFailure(1, invalidEUI)
                }
                and("It's an InvalidClaimLocation failure") {
                    coMockEitherLeft(
                        { usecase.claimDevice(devEUI, location.lat, location.lon, deviceKey) },
                        invalidClaimLocationFailure
                    )
                    testClaimingFailure(2, invalidLocation)
                }
                and("It's an DeviceAlreadyClaimed failure") {
                    coMockEitherLeft(
                        { usecase.claimDevice(devEUI, location.lat, location.lon, deviceKey) },
                        deviceAlreadyClaimedFailure
                    )
                    testClaimingFailure(3, alreadyClaimed)
                }
                and("It's an DeviceNotFound failure") {
                    coMockEitherLeft(
                        { usecase.claimDevice(devEUI, location.lat, location.lon, deviceKey) },
                        deviceNotFoundFailure
                    )
                    testClaimingFailure(4, DEVICE_NOT_FOUND_MSG)
                }
                and("It's an DeviceClaiming failure") {
                    coMockEitherLeft(
                        { usecase.claimDevice(devEUI, location.lat, location.lon, deviceKey) },
                        deviceClaimingFailure
                    )
                    testClaimingFailure(5, deviceClaimingError)
                }
                and("it's any other failure") {
                    coMockEitherLeft(
                        { usecase.claimDevice(devEUI, location.lat, location.lon, deviceKey) },
                        failure
                    )
                    testClaimingFailure(6, REACH_OUT_MSG)
                }
            }
        }
    }

    context("Set a claimed device") {
        given("a device") {
            When("it's null") {
                then("Do nothing. LiveData onClaimResult shouldn't be updated") {
                    viewModel.setClaimedDevice(null)
                    viewModel.onClaimResult().value?.status shouldBe Status.ERROR
                }
            }
            When("it's not null") {
                viewModel.setClaimedDevice(device)
                then("LiveData onClaimResult posts a success with the device") {
                    viewModel.onClaimResult().isSuccess(device)
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
