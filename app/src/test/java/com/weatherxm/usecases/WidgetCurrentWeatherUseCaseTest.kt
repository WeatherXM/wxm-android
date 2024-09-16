package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.Device
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.WidgetRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk

class WidgetCurrentWeatherUseCaseTest : BehaviorSpec({
    val deviceRepository = mockk<DeviceRepository>()
    val widgetRepository = mockk<WidgetRepository>()
    val usecase = WidgetCurrentWeatherUseCaseImpl(deviceRepository, widgetRepository)

    val widgetId = 0
    val deviceId = "deviceId"
    val device = Device.empty()
    val uiDevice = device.toUIDevice()

    beforeSpec {
        coJustRun { widgetRepository.removeWidgetId(widgetId) }
    }

    context("Remove Widget ID") {
        given("A repository that provides the DELETE functionality") {
            then("remove the widget's ID") {
                usecase.removeWidgetId(widgetId)
                coVerify(exactly = 1) { widgetRepository.removeWidgetId(widgetId) }
            }
        }
    }

    context("Get the device associated with a the widget") {
        given("A repository providing the device ID") {
            When("it's a success") {
                coMockEitherRight({ widgetRepository.getWidgetDevice(widgetId) }, deviceId)
                then("return the device ID") {
                    usecase.getWidgetDevice(widgetId) shouldBe deviceId
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ widgetRepository.getWidgetDevice(widgetId) }, failure)
                then("return null") {
                    usecase.getWidgetDevice(widgetId) shouldBe null
                }
            }
        }
    }

    context("Get user device by device ID") {
        given("A repository providing the device") {
            When("it's a success") {
                coMockEitherRight({ deviceRepository.getUserDevice(deviceId) }, device)
                then("return this devices as a UIDevice") {
                    usecase.getUserDevice(deviceId).isSuccess(uiDevice)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ deviceRepository.getUserDevice(deviceId) }, failure)
                then("return that failure") {
                    usecase.getUserDevice(deviceId).isError()
                }
            }
        }
    }
})
