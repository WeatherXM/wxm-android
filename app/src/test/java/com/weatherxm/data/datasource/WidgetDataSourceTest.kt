package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.testGetFromCache
import com.weatherxm.data.services.CacheService
import com.weatherxm.data.services.CacheService.Companion.WIDGET_ID
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class WidgetDataSourceTest : BehaviorSpec({
    val cacheService = mockk<CacheService>()
    val datasource = WidgetDataSourceImpl(cacheService)

    val widgetId = 1
    val otherWidgetId = 2
    val deviceId = "deviceId"
    val widgetCacheKey = "${WIDGET_ID}_${widgetId}"

    beforeSpec {
        every { cacheService.getFollowedDevicesIds() } returns mutableListOf()
        coJustRun { cacheService.setWidgetDevice(widgetCacheKey, deviceId) }
        coJustRun { cacheService.setWidgetIds(any()) }
        coJustRun { cacheService.removeDeviceOfWidget(widgetCacheKey) }
    }

    context("Get the associated device of a widget") {
        given("A Cache Source providing the associated device of a widget") {
            When("Using the Cache Source") {
                testGetFromCache(
                    "widget's device",
                    deviceId,
                    mockFunction = { cacheService.getWidgetDevice(widgetCacheKey) },
                    runFunction = { datasource.getWidgetDevice(widgetId) }
                )
            }
        }
    }

    context("Set the associated device of a widget") {
        given("A Cache Source providing the SET mechanism") {
            When("Using the Cache Source") {
                then("ensure that device is set in the cache") {
                    datasource.setWidgetDevice(widgetId, deviceId)
                    verify(exactly = 1) { cacheService.setWidgetDevice(widgetCacheKey, deviceId) }
                }
            }
        }
    }

    context("Set a widget ID") {
        given("A Cache Source providing the SET mechanism") {
            When("Using the Cache Source") {
                When("there are not any widget IDs saved in the cache") {
                    every { cacheService.getWidgetIds() } returns Either.Left(failure)
                    then("create a new empty list and save that list") {
                        datasource.setWidgetId(widgetId)
                        verify(exactly = 1) {
                            cacheService.setWidgetIds(listOf(widgetId.toString()))
                        }
                    }
                }
                When("there are other widget IDs saved in the cache") {
                    every {
                        cacheService.getWidgetIds()
                    } returns Either.Right(mutableListOf(otherWidgetId.toString()))
                    then("add in this list the widget ID and save that list") {
                        datasource.setWidgetId(widgetId)
                        verify(exactly = 1) {
                            cacheService.setWidgetIds(
                                listOf(otherWidgetId.toString(), widgetId.toString())
                            )
                        }
                    }
                }
            }
        }
    }

    context("Remove a widget") {
        given("A Cache Source providing the REMOVE mechanism") {
            When("Using the Cache Source") {
                When("there are not any widget IDs saved in the cache") {
                    every { cacheService.getWidgetIds() } returns Either.Left(failure)
                    then("remove the associated device of the widget") {
                        datasource.removeWidgetId(widgetId)
                        verify(exactly = 1) { cacheService.removeDeviceOfWidget(widgetCacheKey) }
                    }
                    then("create a new empty list and save that list") {
                        verify(exactly = 1) { cacheService.setWidgetIds(listOf()) }
                    }
                }
                When("the widget ID to be removed is saved in the cache") {
                    every {
                        cacheService.getWidgetIds()
                    } returns Either.Right(mutableListOf(widgetId.toString()))
                    then("remove the widget ID from that list and save it again") {
                        datasource.removeWidgetId(widgetId)
                        verify(exactly = 2) { cacheService.setWidgetIds(listOf()) }
                    }
                }
            }
        }
    }
})
