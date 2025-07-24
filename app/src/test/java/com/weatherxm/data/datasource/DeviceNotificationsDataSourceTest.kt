package com.weatherxm.data.datasource

import com.weatherxm.TestConfig.cacheService
import com.weatherxm.data.models.DeviceNotificationType
import com.weatherxm.data.services.CacheService.Companion.KEY_DEVICE_NOTIFICATION
import com.weatherxm.data.services.CacheService.Companion.KEY_DEVICE_NOTIFICATIONS
import com.weatherxm.data.services.CacheService.Companion.KEY_DEVICE_NOTIFICATION_TYPES
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.verify

class DeviceNotificationsDataSourceTest : BehaviorSpec({
    val datasource = DeviceNotificationsDataSourceImpl(cacheService)

    val timestamp = System.currentTimeMillis()
    val deviceId = "deviceId"
    val notificationTypes =
        setOf(DeviceNotificationType.ACTIVITY.name, DeviceNotificationType.HEALTH.name)

    val notificationsEnabledKey = "${KEY_DEVICE_NOTIFICATIONS}_${deviceId}"
    val notificationTypesKey = "${KEY_DEVICE_NOTIFICATION_TYPES}_${deviceId}"
    val notificationKey = "${KEY_DEVICE_NOTIFICATION}_${DeviceNotificationType.HEALTH}_${deviceId}"

    beforeSpec {
        coJustRun { cacheService.setDeviceNotificationsEnabled(notificationsEnabledKey, true) }
        every { cacheService.getDeviceNotificationsEnabled(notificationsEnabledKey) } returns true
        coJustRun {
            cacheService.setDeviceNotificationTypesEnabled(notificationTypesKey, notificationTypes)
        }
        every { cacheService.getDeviceNotificationTypeTimestamp(notificationKey) } returns timestamp
        coJustRun { cacheService.setDeviceNotificationTypeTimestamp(notificationKey, any()) }
        every {
            cacheService.getDeviceNotificationTypesEnabled(notificationTypesKey)
        } returns notificationTypes
        every { cacheService.getDeviceNotificationsPrompt() } returns true
        coJustRun { cacheService.checkDeviceNotificationsPrompt() }
    }

    context("GET / SET if the device notifications are enabled") {
        When("GET") {
            then("return if the notifications are enabled") {
                datasource.getDeviceNotificationsEnabled(deviceId) shouldBe true
            }
        }
        When("SET") {
            then("set the notifications enabled flag") {
                datasource.setDeviceNotificationsEnabled(deviceId, true)
                verify(exactly = 1) {
                    cacheService.setDeviceNotificationsEnabled(notificationsEnabledKey, true)
                }
            }
        }
    }

    context("GET / SET the types of device notifications that are enabled") {
        When("GET") {
            then("return those types") {
                datasource.getDeviceNotificationTypesEnabled(deviceId) shouldBe notificationTypes
            }
        }
        When("SET") {
            then("set those types") {
                datasource.setDeviceNotificationTypesEnabled(deviceId, notificationTypes)
                verify(exactly = 1) {
                    cacheService.setDeviceNotificationTypesEnabled(
                        notificationTypesKey,
                        notificationTypes
                    )
                }
            }
        }
    }

    context("GET / SET if the notifications prompt should be shown") {
        When("GET") {
            then("return if the prompt should be shown") {
                datasource.showDeviceNotificationsPrompt() shouldBe true
            }
        }
        When("SET") {
            then("check that the prompt is set") {
                datasource.checkDeviceNotificationsPrompt()
                verify(exactly = 1) { cacheService.checkDeviceNotificationsPrompt() }
            }
        }
    }

    context("GET / SET the timestamp of a specific device notification type") {
        When("GET") {
            then("return that timestamp") {
                datasource.getDeviceNotificationTypeTimestamp(
                    deviceId, DeviceNotificationType.HEALTH
                ) shouldBe timestamp
            }
        }
        When("SET") {
            then("set that timestamp") {
                datasource.setDeviceNotificationTypeTimestamp(
                    deviceId,
                    DeviceNotificationType.HEALTH
                )
                verify(exactly = 1) {
                    cacheService.setDeviceNotificationTypeTimestamp(notificationKey, any())
                }
            }
        }
    }
})
