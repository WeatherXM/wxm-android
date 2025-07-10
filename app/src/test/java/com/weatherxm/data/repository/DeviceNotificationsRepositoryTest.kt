package com.weatherxm.data.repository

import com.weatherxm.data.datasource.DeviceNotificationsDataSource
import com.weatherxm.data.models.DeviceNotificationType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class DeviceNotificationsRepositoryTest : BehaviorSpec({
    val dataSource = mockk<DeviceNotificationsDataSource>()
    val repository = DeviceNotificationsRepositoryImpl(dataSource)

    val deviceId = "deviceId"
    val notificationTypes = setOf(DeviceNotificationType.ACTIVITY, DeviceNotificationType.HEALTH)
    val typesAsStringSet = notificationTypes.map { it.name }.toSet()

    beforeSpec {
        coJustRun { dataSource.setDeviceNotificationsEnabled(deviceId, true) }
        every { dataSource.getDeviceNotificationsEnabled(deviceId) } returns true
        coJustRun { dataSource.setDeviceNotificationTypesEnabled(deviceId, typesAsStringSet) }
        every { dataSource.getDeviceNotificationTypesEnabled(deviceId) } returns typesAsStringSet
        every { dataSource.showDeviceNotificationsPrompt() } returns true
        coJustRun { dataSource.checkDeviceNotificationsPrompt() }
    }

    context("GET / SET if the device notifications are enabled") {
        When("GET") {
            then("return if the notifications are enabled") {
                repository.getDeviceNotificationsEnabled(deviceId) shouldBe true
            }
        }
        When("SET") {
            then("set the notifications enabled flag") {
                repository.setDeviceNotificationsEnabled(deviceId, true)
                verify(exactly = 1) {
                    dataSource.setDeviceNotificationsEnabled(deviceId, true)
                }
            }
        }
    }

    context("GET / SET the types of device notifications that are enabled") {
        When("GET") {
            then("return those types") {
                repository.getDeviceNotificationTypesEnabled(deviceId) shouldBe notificationTypes
            }
        }
        When("SET") {
            then("set those types") {
                repository.setDeviceNotificationTypesEnabled(deviceId, notificationTypes)
                verify(exactly = 1) {
                    dataSource.setDeviceNotificationTypesEnabled(deviceId, typesAsStringSet)
                }
            }
        }
    }


    context("GET / SET if the notifications prompt should be shown") {
        When("GET") {
            then("return if the prompt should be shown") {
                repository.showDeviceNotificationsPrompt() shouldBe true
            }
        }
        When("SET") {
            then("check that the prompt is set") {
                repository.checkDeviceNotificationsPrompt()
                verify(exactly = 1) { dataSource.checkDeviceNotificationsPrompt() }
            }
        }
    }
})
