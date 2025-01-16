package com.weatherxm.data.datasource

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestConfig.cacheService
import com.weatherxm.TestConfig.successUnitResponse
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.data.models.PhotoPresignedMetadata
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ErrorResponse
import com.weatherxm.data.network.PhotoNamesBody
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class DevicePhotoDataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val datasource = DevicePhotoDataSourceImpl(apiService, cacheService)

    val deviceId = "deviceId"
    val photoName = "photoName"
    val emptyListString = listOf<String>()
    val uploadIds = listOf("uploadIds")
    val photosMetadata = listOf<PhotoPresignedMetadata>()
    val devicePhotosResponse = NetworkResponse.Success<List<String>, ErrorResponse>(
        emptyListString,
        retrofitResponse(emptyListString)
    )
    val photosMetadataResponse =
        NetworkResponse.Success<List<PhotoPresignedMetadata>, ErrorResponse>(
            photosMetadata,
            retrofitResponse(photosMetadata)
        )

    beforeSpec {
        justRun { cacheService.setPhotoVerificationAcceptedTerms() }
        every { cacheService.getDevicePhotoUploadIds(deviceId) } returns uploadIds
    }

    context("Get device photos") {
        When("Using the Network Source") {
            testNetworkCall(
                "the list of photos",
                emptyListString,
                devicePhotosResponse,
                mockFunction = { apiService.getDevicePhotos(deviceId) },
                runFunction = { datasource.getDevicePhotos(deviceId) }
            )
        }
    }

    context("Delete device photo") {
        When("Using the Network Source") {
            testNetworkCall(
                "the response of the request indicated by Unit as a success",
                Unit,
                successUnitResponse,
                mockFunction = { apiService.deleteDevicePhoto(deviceId, photoName) },
                runFunction = { datasource.deleteDevicePhoto(deviceId, photoName) }
            )
        }
    }

    context("Get device photos metadata") {
        When("Using the Network Source") {
            testNetworkCall(
                "the list of photos metadata",
                photosMetadata,
                photosMetadataResponse,
                mockFunction = {
                    apiService.getPhotosMetadataForUpload(
                        deviceId,
                        PhotoNamesBody(listOf(photoName))
                    )
                },
                runFunction = { datasource.getPhotosMetadataForUpload(deviceId, listOf(photoName)) }
            )
        }
    }

    context("Get / Set if the user has accepted the uploading photos terms") {
        given("A Cache Source providing the GET / SET mechanisms") {
            When("We should get the user's accepted status") {
                and("user has not accepted the terms") {
                    every { cacheService.getPhotoVerificationAcceptedTerms() } returns false
                    then("return false") {
                        datasource.getAcceptedTerms() shouldBe false
                    }
                }
                and("user has accepted the terms") {
                    every { cacheService.getPhotoVerificationAcceptedTerms() } returns true
                    then("return true") {
                        datasource.getAcceptedTerms() shouldBe true
                    }
                }
            }
            When("We should set that the user has accepted the terms") {
                then("ensure that the SET takes place in the cache") {
                    datasource.setAcceptedTerms()
                    verify(exactly = 1) { cacheService.setPhotoVerificationAcceptedTerms() }
                }
            }
        }
    }

    context("Get device photos upload IDs") {
        given("a device ID") {
            then("return the list of upload IDs") {
                datasource.getDevicePhotoUploadIds(deviceId) shouldBe uploadIds
            }
        }
    }
})
