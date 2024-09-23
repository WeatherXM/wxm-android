package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.CountryAndFrequencies
import com.weatherxm.data.models.DeviceInfo
import com.weatherxm.data.models.Frequency
import com.weatherxm.data.models.Location
import com.weatherxm.data.repository.AddressRepository
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.ui.common.UIDevice
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class StationSettingsUseCaseTest : BehaviorSpec({
    val deviceRepository = mockk<DeviceRepository>()
    val deviceOTARepository = mockk<DeviceOTARepository>()
    val addressRepository = mockk<AddressRepository>()
    val usecase =
        StationSettingsUseCaseImpl(deviceRepository, deviceOTARepository, addressRepository)

    val deviceId = "deviceId"
    val friendlyName = "This is a friendly name!"
    val serialNumber = "serialNumber"
    val lat = 0.0
    val lon = 0.0
    val uiDevice = UIDevice.empty()
    val countryAndFrequencies = CountryAndFrequencies("Greece", Frequency.EU868, listOf())
    val deviceInfo = mockk<DeviceInfo>()

    context("Set / Clear Friendly Name") {
        given("A repository providing the SET FRIENDLY NAME mechanism") {
            When("it's a success") {
                coMockEitherRight(
                    { deviceRepository.setFriendlyName(deviceId, friendlyName) }, Unit
                )
                then("return the success") {
                    usecase.setFriendlyName(deviceId, friendlyName).isSuccess(Unit)
                }
            }
            When("it's a failure") {
                coMockEitherLeft(
                    { deviceRepository.setFriendlyName(deviceId, friendlyName) }, failure
                )
                then("return that failure") {
                    usecase.setFriendlyName(deviceId, friendlyName).isError()
                }
            }
        }
        given("A repository providing the CLEAR FRIENDLY NAME mechanism") {
            When("it's a success") {
                coMockEitherRight({ deviceRepository.clearFriendlyName(deviceId) }, Unit)
                then("return the success") {
                    usecase.clearFriendlyName(deviceId).isSuccess(Unit)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ deviceRepository.clearFriendlyName(deviceId) }, failure)
                then("return that failure") {
                    usecase.clearFriendlyName(deviceId).isError()
                }
            }
        }
    }

    context("Remove a device") {
        given("A repository providing the REMOVE DEVICE mechanism") {
            When("it's a success") {
                coMockEitherRight({ deviceRepository.removeDevice(serialNumber, deviceId) }, Unit)
                then("return the success") {
                    usecase.removeDevice(serialNumber, deviceId).isSuccess(Unit)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ deviceRepository.removeDevice(serialNumber, deviceId) }, failure)
                then("return that failure") {
                    usecase.removeDevice(serialNumber, deviceId).isError()
                }
            }
        }
    }

    context("Get if there should be a notification for OTA for a device") {
        given("A repository providing the answer") {
            coEvery {
                deviceOTARepository.shouldNotifyOTA(uiDevice.id, uiDevice.assignedFirmware)
            } returns true
            then("return the answer") {
                usecase.shouldNotifyOTA(uiDevice) shouldBe true
            }
        }
    }

    context("Get CountryAndFrequencies for lat-lon") {
        given("A repository providing the answer") {
            When("lat is null") {
                then("return the default answer") {
                    usecase.getCountryAndFrequencies(
                        null,
                        lon
                    ) shouldBe CountryAndFrequencies.default()
                }

            }
            When("lon is null") {
                then("return the default answer") {
                    usecase.getCountryAndFrequencies(
                        lon,
                        null
                    ) shouldBe CountryAndFrequencies.default()
                }
            }
            When("lat and lon are valid and non-null") {
                then("return the answer") {
                    coEvery {
                        addressRepository.getCountryAndFrequencies(Location(lat, lon))
                    } returns countryAndFrequencies
                    usecase.getCountryAndFrequencies(lat, lon) shouldBe countryAndFrequencies
                }
            }
        }
    }

    context("Get Device Info") {
        given("A repository providing the data") {
            When("it's a success") {
                coMockEitherRight({ deviceRepository.getDeviceInfo(deviceId) }, deviceInfo)
                then("return that data") {
                    usecase.getDeviceInfo(deviceId).isSuccess(deviceInfo)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ deviceRepository.getDeviceInfo(deviceId) }, failure)
                then("return that failure") {
                    usecase.getDeviceInfo(deviceId).isError()
                }
            }
        }
    }
})
