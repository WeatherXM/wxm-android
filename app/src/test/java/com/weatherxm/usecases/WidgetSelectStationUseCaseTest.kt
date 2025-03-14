package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.Device
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.WidgetRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk

class WidgetSelectStationUseCaseTest : BehaviorSpec({
    val deviceRepository = mockk<DeviceRepository>()
    val widgetRepository = mockk<WidgetRepository>()
    val usecase = WidgetSelectStationUseCaseImpl(deviceRepository, widgetRepository)

    val widgetId = 0
    val deviceId = "deviceId"
    val devices = listOf(Device.empty(), Device.empty())
    val uiDevices = devices.map { it.toUIDevice() }

    beforeSpec {
        coJustRun { widgetRepository.setWidgetDevice(widgetId, deviceId) }
        coJustRun { widgetRepository.setWidgetId(widgetId) }
    }

    context("Get user devices") {
        given("A repository providing the devices") {
            When("it's a success") {
                coMockEitherRight({ deviceRepository.getUserDevices() }, devices)
                then("return these devices as a List<UIDevice>") {
                    usecase.getUserDevices().isSuccess(uiDevices)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ deviceRepository.getUserDevices() }, failure)
                then("return that failure") {
                    usecase.getUserDevices().isError()
                }
            }
        }
    }

    context("Save Widget Data") {
        given("A repository that saves the data") {
            usecase.saveWidgetData(widgetId, deviceId)
            then("save the widget's device") {
                coVerify(exactly = 1) { widgetRepository.setWidgetDevice(widgetId, deviceId) }
            }
            then("save the widget's id") {
                coVerify(exactly = 1) { widgetRepository.setWidgetId(widgetId) }
            }
        }
    }
})
