package com.weatherxm.data.datasource

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestConfig.successUnitResponse
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.TestUtils.testThrowNotImplemented
import com.weatherxm.data.datasource.NetworkDeviceDataSource.Companion.CLAIM_MAX_RETRIES
import com.weatherxm.data.models.Device
import com.weatherxm.data.models.DeviceInfo
import com.weatherxm.data.models.Location
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ClaimDeviceBody
import com.weatherxm.data.network.DeleteDeviceBody
import com.weatherxm.data.network.ErrorResponse
import com.weatherxm.data.network.ErrorResponse.Companion.DEVICE_CLAIMING
import com.weatherxm.data.network.FriendlyNameBody
import com.weatherxm.data.network.LocationBody
import com.weatherxm.data.services.CacheService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class DeviceDataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val cache = mockk<CacheService>()
    val networkSource = NetworkDeviceDataSource(apiService)
    val cacheSource = CacheDeviceDataSource(cache)

    val deviceId = "deviceId"
    val serialNumber = "serialNumber"
    val invalidSerialNumber = "invalidSerialNumber"
    val secret = "secret"
    val location = Location(0.0, 0.0)
    val friendlyName = "friendlyName"

    val device = Device.empty()
    val deviceInfo = mockk<DeviceInfo>()
    val devicesIds = listOf(deviceId)

    val deviceResponse = NetworkResponse.Success<Device, ErrorResponse>(
        device, retrofitResponse(device)
    )
    val devicesListResponse = NetworkResponse.Success<List<Device>, ErrorResponse>(
        listOf(device),
        retrofitResponse(listOf(device))
    )
    val deviceInfoResponse = NetworkResponse.Success<DeviceInfo, ErrorResponse>(
        deviceInfo,
        retrofitResponse(deviceInfo)
    )

    val deviceClaimingErrorResponse = NetworkResponse.ServerError<Device, ErrorResponse>(
        ErrorResponse(DEVICE_CLAIMING, "", "", ""),
        retrofitResponse(device)
    )

    beforeSpec {
        every { cache.getUserDevicesIds() } returns devicesIds
        justRun { cache.setUserDevicesIds(devicesIds) }
    }

    context("Get user devices") {
        When("Using the Network Source") {
            testNetworkCall(
                "user devices",
                listOf(device),
                devicesListResponse,
                mockFunction = { apiService.getUserDevices() },
                runFunction = { networkSource.getUserDevices() }
            )
        }
        When("Using the Cache Source") {
            testThrowNotImplemented { cacheSource.getUserDevices() }
        }
    }

    context("Get a specific user device") {
        When("Using the Network Source") {
            testNetworkCall(
                "specific user device",
                device,
                deviceResponse,
                mockFunction = { apiService.getUserDevice(deviceId) },
                runFunction = { networkSource.getUserDevice(deviceId) }
            )
        }
        When("Using the Cache Source") {
            testThrowNotImplemented { cacheSource.getUserDevice(deviceId) }
        }
    }

    context("Set the friendly name of a device") {
        When("Using the Network Source") {
            testNetworkCall(
                "Unit",
                Unit,
                successUnitResponse,
                mockFunction = {
                    apiService.setFriendlyName(deviceId, FriendlyNameBody(friendlyName))
                },
                runFunction = { networkSource.setFriendlyName(deviceId, friendlyName) }
            )
        }
        When("Using the Cache Source") {
            testThrowNotImplemented { cacheSource.setFriendlyName(deviceId, friendlyName) }
        }
    }

    context("Clear the friendly name of a device") {
        When("Using the Network Source") {
            testNetworkCall(
                "Unit",
                Unit,
                successUnitResponse,
                mockFunction = { apiService.clearFriendlyName(deviceId) },
                runFunction = { networkSource.clearFriendlyName(deviceId) }
            )
        }
        When("Using the Cache Source") {
            testThrowNotImplemented { cacheSource.clearFriendlyName(deviceId) }
        }
    }

    context("Remove a device") {
        When("Using the Network Source") {
            testNetworkCall(
                "Unit",
                Unit,
                successUnitResponse,
                mockFunction = { apiService.removeDevice(DeleteDeviceBody(serialNumber)) },
                runFunction = { networkSource.removeDevice(serialNumber) }
            )
        }
        When("Using the Cache Source") {
            testThrowNotImplemented { cacheSource.removeDevice(serialNumber) }
        }
    }

    context("Get the Device Info") {
        When("Using the Network Source") {
            testNetworkCall(
                "device info",
                deviceInfo,
                deviceInfoResponse,
                mockFunction = { apiService.getUserDeviceInfo(deviceId) },
                runFunction = { networkSource.getDeviceInfo(deviceId) }
            )
        }
        When("Using the Cache Source") {
            testThrowNotImplemented { cacheSource.getDeviceInfo(deviceId) }
        }
    }

    context("Get the IDs of user devices") {
        When("Using the Network Source") {
            testThrowNotImplemented { networkSource.getUserDevicesIds() }
        }
        When("Using the Cache Source") {
            then("return the list of IDs") {
                cacheSource.getUserDevicesIds() shouldBe devicesIds
            }
        }
    }

    context("Set the IDs of user devices") {
        When("Using the Network Source") {
            testThrowNotImplemented { networkSource.setUserDevicesIds(devicesIds) }
        }
        When("Using the Cache Source") {
            then("set the list of IDs in cache") {
                cacheSource.setUserDevicesIds(devicesIds)
                verify(exactly = 1) { cache.setUserDevicesIds(devicesIds) }
            }
        }
    }

    context("Set the location of a device") {
        When("Using the Network Source") {
            testNetworkCall(
                "device",
                device,
                deviceResponse,
                mockFunction = {
                    apiService.setLocation(deviceId, LocationBody(location.lat, location.lon))
                },
                runFunction = { networkSource.setLocation(deviceId, location.lat, location.lon) }
            )
        }
        When("Using the Cache Source") {
            testThrowNotImplemented {
                cacheSource.setLocation(deviceId, location.lat, location.lon)
            }
        }
    }

    context("Claim a device") {
        When("Using the Network Source") {
            and("it's a DeviceClaiming failure") {
                coEvery {
                    apiService.claimDevice(ClaimDeviceBody(invalidSerialNumber, location, secret))
                } returns deviceClaimingErrorResponse
                then("retry until [CLAIM_MAX_RETRIES = $CLAIM_MAX_RETRIES] is hit") {
                    networkSource.claimDevice(invalidSerialNumber, location, secret)
                    coVerify(exactly = CLAIM_MAX_RETRIES + 1) {
                        apiService.claimDevice(
                            ClaimDeviceBody(invalidSerialNumber, location, secret)
                        )
                    }
                }
            }
            and("it's either a success or a failure other than DeviceClaiming") {
                testNetworkCall(
                    "device",
                    device,
                    deviceResponse,
                    mockFunction = {
                        apiService.claimDevice(ClaimDeviceBody(serialNumber, location, secret))
                    },
                    runFunction = { networkSource.claimDevice(serialNumber, location, secret) }
                )
            }
        }
        When("Using the Cache Source") {
            testThrowNotImplemented { cacheSource.claimDevice(serialNumber, location) }
        }
    }
})
