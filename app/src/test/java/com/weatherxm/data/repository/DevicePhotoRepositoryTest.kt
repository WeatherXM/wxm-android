package com.weatherxm.data.repository

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.datasource.DevicePhotoDataSource
import com.weatherxm.data.models.DevicePhoto
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class DevicePhotoRepositoryTest : BehaviorSpec({
    val dataSource = mockk<DevicePhotoDataSource>()
    val repo = DevicePhotoRepositoryImpl(dataSource)

    val deviceId = "deviceId"
    val devicePhotos = listOf<DevicePhoto>()

    beforeSpec {
        justRun { dataSource.setAcceptedTerms() }
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
})
