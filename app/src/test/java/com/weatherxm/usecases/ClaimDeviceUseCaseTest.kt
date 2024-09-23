package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.CountryAndFrequencies
import com.weatherxm.data.models.Device
import com.weatherxm.data.models.Frequency
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.Relation
import com.weatherxm.data.repository.AddressRepository
import com.weatherxm.data.repository.DeviceRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class ClaimDeviceUseCaseTest : BehaviorSpec({
    val deviceRepo = mockk<DeviceRepository>()
    val addressRepo = mockk<AddressRepository>()
    val usecase = ClaimDeviceUseCaseImpl(deviceRepo, addressRepo)

    val serialNumber = "serialNumber"
    val lat = 0.0
    val lon = 0.0
    val device = Device(
        "ownedId",
        "Device Name",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        Relation.owned,
        null
    )
    val uiDevice = device.toUIDevice()
    val location = Location(lat, lon)
    val countryAndFrequencies = CountryAndFrequencies("Greece", Frequency.EU868, listOf())

    beforeSpec {
        coEvery { addressRepo.getCountryAndFrequencies(location) } returns countryAndFrequencies
    }

    context("Claim a device") {
        given("The repository providing that information") {
            When("the response is a failure") {
                coMockEitherLeft({ deviceRepo.claimDevice(serialNumber, location) }, failure)
                then("return that failure") {
                    usecase.claimDevice(serialNumber, lat, lon).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({ deviceRepo.claimDevice(serialNumber, location) }, device)
                then("return that success") {
                    usecase.claimDevice(serialNumber, lat, lon).isSuccess(uiDevice)
                }
            }

        }
    }

    context("Get Country and Frequencies from Location") {
        given("the repository providing that information") {
            then("return that info") {
                usecase.getCountryAndFrequencies(location) shouldBe countryAndFrequencies
            }
        }
    }
})
