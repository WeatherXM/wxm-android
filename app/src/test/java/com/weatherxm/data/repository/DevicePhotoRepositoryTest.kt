package com.weatherxm.data.repository

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.datasource.DevicePhotoDataSource
import com.weatherxm.data.models.PhotoPresignedMetadata
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest

class DevicePhotoRepositoryTest : BehaviorSpec({
    val dataSource = mockk<DevicePhotoDataSource>()
    val repo = DevicePhotoRepositoryImpl(dataSource)

    val deviceId = "deviceId"
    val uploadId = "uploadId"
    val uploadIds = listOf(uploadId)
    val photoName = "photo.jpg"
    val photoPath = "path/to/$photoName"
    val uuidNameOfPhoto = "72acded3-acd4-3e4c-8b6e-d680854b8ab1.jpg"
    val metadata = listOf<PhotoPresignedMetadata>()
    val devicePhotos = listOf<String>()
    val uploadRequest = mockk<MultipartUploadRequest>()

    beforeSpec {
        justRun { dataSource.setAcceptedTerms() }
        justRun { dataSource.addDevicePhotoUploadIdAndRequest(deviceId, uploadId, uploadRequest) }
        every { dataSource.getDevicePhotoUploadIds(deviceId) } returns uploadIds
        every { dataSource.getUploadIdRequest(uploadId) } returns uploadRequest
    }

    context("Get device photos") {
        given("a device ID") {
            When("the response is a failure") {
                coMockEitherLeft({ dataSource.getDevicePhotos(deviceId) }, failure)
                then("return the failure") {
                    repo.getDevicePhotos(deviceId).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({ dataSource.getDevicePhotos(deviceId) }, devicePhotos)
                then("return the device photos") {
                    repo.getDevicePhotos(deviceId).isSuccess(devicePhotos)
                }
            }
        }
    }

    context("Delete device photo") {
        given("a device ID and a photo path") {
            When("the response is a failure") {
                coMockEitherLeft({ dataSource.deleteDevicePhoto(deviceId, photoName) }, failure)
                then("return the failure") {
                    repo.deleteDevicePhoto(deviceId, photoPath).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({ dataSource.deleteDevicePhoto(deviceId, photoName) }, Unit)
                then("return Unit") {
                    repo.deleteDevicePhoto(deviceId, photoPath).isSuccess(Unit)
                }
            }
        }
    }

    context("Get photos metadata for upload") {
        given("a device ID and a list of photo paths") {
            When("the response is a failure") {
                coMockEitherLeft(
                    { dataSource.getPhotosMetadataForUpload(deviceId, listOf()) },
                    failure
                )
                then("return the failure") {
                    repo.getPhotosMetadataForUpload(deviceId, listOf()).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({
                    dataSource.getPhotosMetadataForUpload(deviceId, listOf(uuidNameOfPhoto))
                }, metadata)
                then("return the device photos metadata") {
                    repo.getPhotosMetadataForUpload(deviceId, listOf(photoPath)).isSuccess(metadata)
                }
            }
        }
    }

    context("Get / Set if the user has accepted the uploading photos terms") {
        given("The data source providing the GET / SET mechanisms") {
            When("We should get the user's accepted status") {
                and("user has not accepted the terms") {
                    every { dataSource.getAcceptedTerms() } returns false
                    then("return false") {
                        repo.getAcceptedTerms() shouldBe false
                    }
                }
                and("user has accepted the terms") {
                    every { dataSource.getAcceptedTerms() } returns true
                    then("return true") {
                        repo.getAcceptedTerms() shouldBe true
                    }
                }
            }
            When("We should set that the user has accepted the terms") {
                then("ensure that the SET takes place in the data source") {
                    repo.setAcceptedTerms()
                    verify(exactly = 1) { dataSource.setAcceptedTerms() }
                }
            }
        }
    }

    context("Get device photos upload IDs") {
        given("a device ID") {
            then("return the list of upload IDs") {
                repo.getDevicePhotoUploadIds(deviceId) shouldBe uploadIds
            }
        }
    }

    context("Add a device photo along with an upload ID") {
        given("save through the respective function") {
            repo.addDevicePhotoUploadIdAndRequest(deviceId, uploadId, uploadRequest)
            then("save them in cache") {
                verify(exactly = 1) {
                    dataSource.addDevicePhotoUploadIdAndRequest(deviceId, uploadId, uploadRequest)
                }
            }
        }
    }

    context("Get the upload ID's request") {
        given("an upload ID") {
            then("return that request") {
                repo.getUploadIdRequest(uploadId) shouldBe uploadRequest
            }
        }
    }
})
