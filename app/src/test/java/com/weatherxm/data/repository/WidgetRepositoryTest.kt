package com.weatherxm.data.repository

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.mockEitherLeft
import com.weatherxm.TestUtils.mockEitherRight
import com.weatherxm.data.datasource.WidgetDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class WidgetRepositoryTest : BehaviorSpec({
    val dataSource = mockk<WidgetDataSource>()
    val repository = WidgetRepositoryImpl(dataSource)

    val widgetId = 1
    val deviceId = "deviceId"

    beforeSpec {
        justRun { dataSource.setWidgetId(widgetId) }
        justRun { dataSource.removeWidgetId(widgetId) }
        justRun { dataSource.setWidgetDevice(widgetId, deviceId) }
    }

    context("Perform Widget-related actions") {
        given("A widget ID") {
            then("The widget ID should be set") {
                repository.setWidgetId(widgetId)
                verify(exactly = 1) { dataSource.setWidgetId(widgetId) }
            }
            then("The widget ID should be removed") {
                repository.removeWidgetId(widgetId)
                verify(exactly = 1) { dataSource.removeWidgetId(widgetId) }
            }
            then("The device ID should be set") {
                repository.setWidgetDevice(widgetId, deviceId)
                verify(exactly = 1) { dataSource.setWidgetDevice(widgetId, deviceId) }
            }
            and("The device ID should be retrieved") {
                When("is a success") {
                    mockEitherRight({ dataSource.getWidgetDevice(widgetId) }, deviceId)
                    then("The device ID should be returned") {
                        repository.getWidgetDevice(widgetId).isSuccess(deviceId)
                    }
                }
                When("is a failure") {
                    mockEitherLeft({ dataSource.getWidgetDevice(widgetId) }, failure)
                    then("The device ID should not be returned") {
                        repository.getWidgetDevice(widgetId).isError()
                    }
                }
            }
        }
    }

})
