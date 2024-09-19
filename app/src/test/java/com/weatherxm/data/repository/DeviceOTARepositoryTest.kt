package com.weatherxm.data.repository

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.mockEitherLeft
import com.weatherxm.TestUtils.mockEitherRight
import com.weatherxm.data.datasource.DeviceOTADataSource
import com.weatherxm.data.repository.DeviceOTARepositoryImpl.Companion.OTA_UPDATE_HIDE_EXPIRATION
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class DeviceOTARepositoryTest : BehaviorSpec({
    val dataSource = mockk<DeviceOTADataSource>()
    val repo = DeviceOTARepositoryImpl(dataSource)

    val deviceId = "deviceId"
    val version = "1.0.0"
    val version2 = "2.0.0"
    val firmware = ByteArray(1)
    val now = System.currentTimeMillis()
    val otaNotificationThresholdExpired = now - (OTA_UPDATE_HIDE_EXPIRATION + 1)

    beforeSpec {
        justRun { dataSource.setDeviceLastOtaVersion(deviceId, version) }
        justRun { dataSource.setDeviceLastOtaTimestamp(deviceId) }
    }

    context("Perform OTA related actions") {
        given("A device ID") {
            and("Get Firmware") {
                When("Firmware is available") {
                    coMockEitherRight({ dataSource.getFirmware(deviceId) }, firmware)
                    then("return the firmware") {
                        repo.getFirmware(deviceId).isSuccess(firmware)
                    }
                }
                When("Firmware is NOT available") {
                    coMockEitherLeft({ dataSource.getFirmware(deviceId) }, failure)
                    then("return null") {
                        repo.getFirmware(deviceId).isError()
                    }
                }
            }
            and("Trigger on a successful update") {
                then("Set last OTA version & timestamp information") {
                    repo.onUpdateSuccess(deviceId, version)
                    verify(exactly = 1) { dataSource.setDeviceLastOtaVersion(deviceId, version) }
                    verify(exactly = 1) { dataSource.setDeviceLastOtaTimestamp(deviceId) }
                }
            }
            and("Check if user should be notified of OTA") {
                When("OTA version is null or empty") {
                    then("return false") {
                        repo.shouldNotifyOTA(deviceId, null) shouldBe false
                        repo.shouldNotifyOTA(deviceId, "") shouldBe false
                    }
                }
                When("OTA version is valid") {
                    and("There isn't any other OTA version we have notified the user before") {
                        mockEitherLeft({ dataSource.getDeviceLastOtaVersion(deviceId) }, failure)
                        then("return true") {
                            repo.shouldNotifyOTA(deviceId, version) shouldBe true
                        }
                    }
                    and("There is another OTA version we have notified the user before") {
                        and("OTA version == as the last OTA version notified of") {
                            mockEitherRight(
                                { dataSource.getDeviceLastOtaVersion(deviceId) },
                                version
                            )
                            and("Notification Threshold has expired") {
                                every {
                                    dataSource.getDeviceLastOtaTimestamp(deviceId)
                                } returns otaNotificationThresholdExpired
                                then("return true") {
                                    repo.shouldNotifyOTA(deviceId, version) shouldBe true
                                }
                            }
                            and("Notification Threshold has NOT expired") {
                                every { dataSource.getDeviceLastOtaTimestamp(deviceId) } returns now
                                then("return false") {
                                    repo.shouldNotifyOTA(deviceId, version) shouldBe false
                                }
                            }
                        }
                        and("OTA version != than the last OTA version notified of") {
                            mockEitherRight(
                                { dataSource.getDeviceLastOtaVersion(deviceId) },
                                version
                            )
                            then("return true") {
                                repo.shouldNotifyOTA(deviceId, version2) shouldBe true
                            }
                        }
                    }
                }
            }
        }
    }

})
