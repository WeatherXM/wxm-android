package com.weatherxm.ui.devicenotifications

import com.weatherxm.data.models.DeviceNotificationType
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.DeviceNotificationsUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class DeviceNotificationsViewModelTest : BehaviorSpec({
    val usecase = mockk<DeviceNotificationsUseCase>()
    lateinit var viewModel: DeviceNotificationsViewModel

    val device = UIDevice.empty()
    val activityType = DeviceNotificationType.ACTIVITY
    val batteryType = DeviceNotificationType.BATTERY
    val types = setOf(activityType, batteryType)

    beforeSpec {
        justRun { usecase.setDeviceNotificationsEnabled(device.id, true) }
        every { usecase.getDeviceNotificationsEnabled(device.id) } returns true
        every { usecase.getDeviceNotificationTypesEnabled(device.id) } returns types
        justRun { usecase.setDeviceNotificationTypesEnabled(device.id, any()) }

        viewModel = DeviceNotificationsViewModel(device, usecase)
    }

    context("GET and SET if the notifications are enabled") {
        When("GET if they are enabled") {
            and("the user hasn't given the respective permission") {
                viewModel.getDeviceNotificationsEnabled(false)
                then("return false") {
                    viewModel.notificationsEnabled.value shouldBe false
                }
            }
            and("the user has given the respective permission") {
                viewModel.getDeviceNotificationsEnabled(true)
                then("return true") {
                    viewModel.notificationsEnabled.value shouldBe true
                }
            }
        }
        When("SET that the notifications are enabled") {
            then("call the respective function") {
                viewModel.setDeviceNotificationsEnabled(true)
            }
            then("GET the property again") {
                viewModel.notificationsEnabled.value shouldBe true
            }
        }
    }

    context("GET and SET the notification types") {
        When("GET the current notification types") {
            then("return those types") {
                viewModel.getDeviceNotificationTypes()
                viewModel.notificationTypesEnabled.toSet() shouldBe types
            }
        }
        When("SET the updated notification types") {
            and("remove a type") {
                viewModel.setDeviceNotificationTypeEnabled(DeviceNotificationType.ACTIVITY, false)
                then("GET the types again") {
                    viewModel.notificationTypesEnabled.toSet() shouldBe setOf(batteryType)
                }
                then("ensure that the usecase was called") {
                    verify(exactly = 1) {
                        usecase.setDeviceNotificationTypesEnabled(device.id, setOf(batteryType))
                    }
                }
            }
            and("add a type") {
                viewModel.setDeviceNotificationTypeEnabled(DeviceNotificationType.ACTIVITY, true)
                then("GET the types again") {
                    viewModel.notificationTypesEnabled.toSet() shouldBe types
                }
                then("ensure that the usecase was called") {
                    verify(exactly = 1) {
                        usecase.setDeviceNotificationTypesEnabled(device.id, types)
                    }
                }
            }
        }
    }
})
