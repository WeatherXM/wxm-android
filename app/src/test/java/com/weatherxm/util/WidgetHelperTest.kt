package com.weatherxm.util

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Parcelable
import arrow.core.Either
import com.weatherxm.R
import com.weatherxm.TestConfig.context
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.mockEitherRight
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.widgets.WidgetType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify

class WidgetHelperTest : BehaviorSpec({
    val cacheService = mockk<CacheService>()
    val appWidgetManager = mockk<AppWidgetManager>()
    val appWidgetProviderInfo = mockk<AppWidgetProviderInfo>()
    val widgetHelper = WidgetHelper(cacheService, context)

    val testDeviceId = "deviceId"
    val testWidgetIds = listOf("1")
    val testWidgetIdNullInfo = -1

    beforeSpec {
        mockEitherRight({ cacheService.getWidgetIds() }, testWidgetIds)
        every { appWidgetManager.getAppWidgetInfo(any()) } returns appWidgetProviderInfo
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(context) } returns appWidgetManager

        mockkConstructor(Intent::class)
        every {
            anyConstructed<Intent>().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, any() as Int)
        } returns mockk()
        every {
            anyConstructed<Intent>().putExtra(Contracts.ARG_WIDGET_TYPE, any() as Parcelable)
        } returns mockk()
        every {
            anyConstructed<Intent>().putExtra(Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE, true)
        } returns mockk()
        every {
            anyConstructed<Intent>().putExtra(Contracts.ARG_WIDGET_SHOULD_SELECT_STATION, true)
        } returns mockk()
        every { context.sendBroadcast(any()) } just runs
    }

    suspend fun BehaviorSpecWhenContainerScope.testWidgetTypeById(
        initialLayout: Int,
        expectedType: WidgetType
    ) {
        appWidgetProviderInfo.initialLayout = initialLayout
        then("getWidgetTypeById should return $expectedType") {
            widgetHelper.getWidgetTypeById(appWidgetManager, 0) shouldBe expectedType
        }
    }

    context("Get widget-related information") {
        given("Some widget IDs") {
            then("getWidgetIds should return a list of widget IDs") {
                widgetHelper.getWidgetIds().isSuccess(testWidgetIds)
            }
        }
        given("A widget layout associated with a widget") {
            When(" is widget_current_weather") {
                testWidgetTypeById(R.layout.widget_current_weather, WidgetType.CURRENT_WEATHER)
            }
            When("is widget_current_weather_tile") {
                testWidgetTypeById(
                    R.layout.widget_current_weather_tile, WidgetType.CURRENT_WEATHER_TILE
                )
            }
            When("is widget_current_weather_detailed") {
                testWidgetTypeById(
                    R.layout.widget_current_weather_detailed, WidgetType.CURRENT_WEATHER_DETAILED
                )
            }
            When("is unknown") {
                testWidgetTypeById(0, WidgetType.CURRENT_WEATHER)
            }
        }
        given("A widget ID with null info") {
            every { appWidgetManager.getAppWidgetInfo(testWidgetIdNullInfo) } returns null
            then("getWidgetTypeById should return CURRENT_WEATHER") {
                widgetHelper.getWidgetTypeById(appWidgetManager, testWidgetIdNullInfo) shouldBe null
            }
        }
    }

    context("Handle an unfollow event") {
        given("A device ID") {
            When("this device has no widgets") {
                every { cacheService.getWidgetDevice(any()) } returns Either.Left(mockk())
                then("the widgets should not be updated") {
                    widgetHelper.onUnfollowEvent(testDeviceId)
                    verify(exactly = 0) { context.sendBroadcast(any()) }
                }
            }
            When("this device has widgets") {
                every { cacheService.getWidgetDevice(any()) } returns Either.Right(testDeviceId)
                then("the widgets should be updated") {
                    widgetHelper.onUnfollowEvent(testDeviceId)
                    verify(exactly = 1) {
                        anyConstructed<Intent>().putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID, testWidgetIds.first().toInt()
                        )
                    }
                    verify(exactly = 1) {
                        anyConstructed<Intent>().putExtra(
                            Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE, true
                        )
                    }
                    verify(exactly = 1) {
                        anyConstructed<Intent>().putExtra(
                            Contracts.ARG_WIDGET_SHOULD_SELECT_STATION, true
                        )
                    }
                    verify(exactly = 1) { context.sendBroadcast(any()) }
                }
            }
        }
    }
})
