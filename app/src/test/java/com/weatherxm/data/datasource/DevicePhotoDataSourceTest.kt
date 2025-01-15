package com.weatherxm.data.datasource

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestConfig.cacheService
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ErrorResponse
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
    val devicePhotos = listOf<String>()
    val photosResponse = NetworkResponse.Success<List<String>, ErrorResponse>(
        devicePhotos,
        retrofitResponse(devicePhotos)
    )

    beforeSpec {
        justRun { cacheService.setPhotoVerificationAcceptedTerms() }
    }

    context("Get device photos") {
        When("Using the Network Source") {
            testNetworkCall(
                "the list of photos",
                devicePhotos,
                photosResponse,
                mockFunction = { apiService.getDevicePhotos(deviceId) },
                runFunction = { datasource.getDevicePhotos(deviceId) }
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
})
