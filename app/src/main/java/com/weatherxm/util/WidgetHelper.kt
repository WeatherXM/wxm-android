package com.weatherxm.util

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import arrow.core.Either
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.widgets.WidgetType

class WidgetHelper(private val cacheService: CacheService, private val context: Context) {
    fun getWidgetIds(): Either<Failure, List<String>> {
        return cacheService.getWidgetIds()
    }

    fun getWidgetTypeById(appWidgetManager: AppWidgetManager, appWidgetId: Int): WidgetType? {
        val widgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        return widgetInfo?.let {
            when (it.initialLayout) {
                R.layout.widget_current_weather -> WidgetType.CURRENT_WEATHER
                R.layout.widget_current_weather_tile -> WidgetType.CURRENT_WEATHER_TILE
                R.layout.widget_current_weather_detailed -> WidgetType.CURRENT_WEATHER_DETAILED
                else -> WidgetType.CURRENT_WEATHER
            }
        }
    }

    fun onUnfollowEvent(deviceId: String) {
        getWidgetIds().onRight {
            val widgetsOfDevice = it.filter { widgetId ->
                cacheService.getWidgetDevice(
                    CacheService.getWidgetFormattedKey(widgetId.toInt())
                ).getOrNull() == deviceId
            }
            widgetsOfDevice.forEach { widgetId ->
                with(Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId.toInt())
                    putExtra(
                        Contracts.ARG_WIDGET_TYPE,
                        getWidgetTypeById(
                            AppWidgetManager.getInstance(context), widgetId.toInt()
                        ) as Parcelable
                    )
                    putExtra(Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE, true)
                    putExtra(Contracts.ARG_WIDGET_SHOULD_SELECT_STATION, true)
                    context.sendBroadcast(this)
                }
            }
        }
    }
}
