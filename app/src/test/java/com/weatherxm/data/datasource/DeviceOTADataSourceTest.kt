package com.weatherxm.data.datasource

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestConfig.cacheService
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testGetFromCache
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.data.datasource.DeviceOTADataSourceImpl.Companion.LAST_OTA_TIMESTAMP
import com.weatherxm.data.datasource.DeviceOTADataSourceImpl.Companion.LAST_OTA_VERSION
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ErrorResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.ResponseBody

class DeviceOTADataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val datasource = DeviceOTADataSourceImpl(apiService, cacheService)

    val deviceId = "deviceId"
    val otaVersion = "otaVersion"
    val timestamp = System.currentTimeMillis()

    val firmware = mockk<ResponseBody>()
    val bytes = ByteArray(0)
    val firmwareResponse =
        NetworkResponse.Success<ResponseBody, ErrorResponse>(firmware, retrofitResponse(firmware))
    val otaVersionCacheKey = "${LAST_OTA_VERSION}_${deviceId}"
    val otaTimestampCacheKey = "${LAST_OTA_TIMESTAMP}_${deviceId}"

    beforeSpec {
        every { firmware.bytes() } returns bytes
        coJustRun { cacheService.setDeviceLastOtaVersion(otaVersionCacheKey, otaVersion) }
        coJustRun { cacheService.setDeviceLastOtaTimestamp(otaTimestampCacheKey, any()) }
        every { cacheService.getDeviceLastOtaTimestamp(otaTimestampCacheKey) } returns timestamp
    }

    context("Get firmware") {
        When("Using the Network Source") {
            testNetworkCall(
                "firmware as ByteArray",
                bytes,
                firmwareResponse,
                mockFunction = { apiService.getFirmware(deviceId) },
                runFunction = { datasource.getFirmware(deviceId) }
            )
        }
    }

    context("Get / Set device's last OTA version that has been notified of") {
        given("A Cache Source providing the GET / SET mechanisms") {
            When("We should get the last OTA version") {
                testGetFromCache(
                    "last OTA version",
                    otaVersion,
                    mockFunction = { cacheService.getDeviceLastOtaVersion(otaVersionCacheKey) },
                    runFunction = { datasource.getDeviceLastOtaVersion(deviceId) }
                )
            }
            When("We should set the last OTA version") {
                then("ensure that the SET takes place in the cache") {
                    datasource.setDeviceLastOtaVersion(deviceId, otaVersion)
                    verify(exactly = 1) {
                        cacheService.setDeviceLastOtaVersion(otaVersionCacheKey, otaVersion)
                    }
                }
            }
        }
    }

    context("Get / Set device's last OTA's timestamp that has been notified of") {
        given("A Cache Source providing the GET / SET mechanisms") {
            When("We should get the last timestamp") {
                datasource.getDeviceLastOtaTimestamp(deviceId) shouldBe timestamp
            }
            When("We should set the last OTA timestamp") {
                then("ensure that the SET takes place in the cache") {
                    datasource.setDeviceLastOtaTimestamp(deviceId)
                    verify(exactly = 1) {
                        cacheService.setDeviceLastOtaTimestamp(otaTimestampCacheKey, any())
                    }
                }
            }
        }
    }
})
