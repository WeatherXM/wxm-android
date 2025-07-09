package com.weatherxm.usecases

import com.weatherxm.data.models.DeviceNotificationType
import com.weatherxm.data.repository.DeviceNotificationsRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class DeviceNotificationsUseCaseTest : BehaviorSpec({
    val repo = mockk<DeviceNotificationsRepository>()
    val usecase = DeviceNotificationsUseCaseImpl(repo)

    val deviceId = "deviceId"
    val notificationTypes = setOf(DeviceNotificationType.ACTIVITY, DeviceNotificationType.HEALTH)

    beforeSpec {
        coJustRun { repo.setDeviceNotificationsEnabled(deviceId, true) }
        every { repo.getDeviceNotificationsEnabled(deviceId) } returns true
        coJustRun { repo.setDeviceNotificationTypesEnabled(deviceId, notificationTypes) }
        every { repo.getDeviceNotificationTypesEnabled(deviceId) } returns notificationTypes
    }

    context("GET / SET if the device notifications are enabled") {
        When("GET") {
            then("return if the notifications are enabled") {
                usecase.getDeviceNotificationsEnabled(deviceId) shouldBe true
            }
        }
        When("SET") {
            then("set the notifications enabled flag") {
                usecase.setDeviceNotificationsEnabled(deviceId, true)
                verify(exactly = 1) {
                    repo.setDeviceNotificationsEnabled(deviceId, true)
                }
            }
        }
    }

    context("GET / SET the types of device notifications that are enabled") {
        When("GET") {
            then("return those types") {
                usecase.getDeviceNotificationTypesEnabled(deviceId) shouldBe notificationTypes
            }
        }
        When("SET") {
            then("set those types") {
                usecase.setDeviceNotificationTypesEnabled(deviceId, notificationTypes)
                verify(exactly = 1) {
                    repo.setDeviceNotificationTypesEnabled(deviceId, notificationTypes)
                }
            }
        }
    }
})
