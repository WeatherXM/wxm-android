package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.PhotoPresignedMetadata
import com.weatherxm.data.repository.DevicePhotoRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class DevicePhotoUseCaseTest : BehaviorSpec({
    val repo = mockk<DevicePhotoRepository>()
    val usecase = DevicePhotoUseCaseImpl(repo)

    val deviceId = "deviceId"
    val photoPath = "photoPath"
    val devicePhotos = listOf<String>()
    val uploadIds = listOf("uploadId")
    val photoMetadata = listOf<PhotoPresignedMetadata>()

    beforeSpec {
        justRun { repo.setAcceptedTerms() }
        every { repo.getDevicePhotoUploadIds(deviceId) } returns uploadIds
    }

    context("Get device photos") {
        given("a device ID") {
            When("the response is a failure") {
                coMockEitherLeft({ repo.getDevicePhotos(deviceId) }, failure)
                then("return the failure") {
                    usecase.getDevicePhotos(deviceId).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({ repo.getDevicePhotos(deviceId) }, devicePhotos)
                then("return the device photos (empty list)") {
                    usecase.getDevicePhotos(deviceId).isSuccess(devicePhotos)
                }
            }
        }
    }

    context("Delete device photo") {
        given("a device ID and a path") {
            When("the response is a failure") {
                coMockEitherLeft({ repo.deleteDevicePhoto(deviceId, photoPath) }, failure)
                then("return the failure") {
                    usecase.deleteDevicePhoto(deviceId, photoPath).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({ repo.deleteDevicePhoto(deviceId, photoPath) }, Unit)
                then("return Unit") {
                    usecase.deleteDevicePhoto(deviceId, photoPath).isSuccess(Unit)
                }
            }
        }
    }

    context("Get photos metadata") {
        given("a device ID and a list of photo paths") {
            When("the response is a failure") {
                coMockEitherLeft(
                    { repo.getPhotosMetadataForUpload(deviceId, listOf(photoPath)) },
                    failure
                )
                then("return the failure") {
                    usecase.getPhotosMetadataForUpload(deviceId, listOf(photoPath)).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight(
                    { repo.getPhotosMetadataForUpload(deviceId, listOf(photoPath)) },
                    photoMetadata
                )
                then("return the device photos metadata") {
                    usecase.getPhotosMetadataForUpload(deviceId, listOf(photoPath))
                        .isSuccess(photoMetadata)
                }
            }
        }
    }

    context("Get device photos upload IDs") {
        given("a device ID") {
            then("return the list of upload IDs") {
                usecase.getDevicePhotoUploadIds(deviceId) shouldBe uploadIds
            }
        }
    }

    context("Get / Set if the user has accepted the uploading photos terms") {
        given("The repository providing the GET / SET mechanisms") {
            When("We should get the user's accepted status") {
                and("user has not accepted the terms") {
                    every { repo.getAcceptedTerms() } returns false
                    then("return false") {
                        usecase.getAcceptedTerms() shouldBe false
                    }
                }
                and("user has accepted the terms") {
                    every { repo.getAcceptedTerms() } returns true
                    then("return true") {
                        usecase.getAcceptedTerms() shouldBe true
                    }
                }
            }
            When("We should set that the user has accepted the terms") {
                then("ensure that the SET takes place in the repository") {
                    usecase.setAcceptedTerms()
                    verify(exactly = 1) { repo.setAcceptedTerms() }
                }
            }
        }
    }
})
