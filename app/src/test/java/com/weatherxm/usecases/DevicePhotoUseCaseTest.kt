package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.DevicePhoto
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
    val devicePhotos = listOf<DevicePhoto>()

    beforeSpec {
        justRun { repo.setAcceptedTerms() }
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
                then("return the device photos") {
                    usecase.getDevicePhotos(deviceId).isSuccess(devicePhotos)
                }
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
