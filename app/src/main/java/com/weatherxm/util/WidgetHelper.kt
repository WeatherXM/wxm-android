package com.weatherxm.util

import android.appwidget.AppWidgetManager
import arrow.core.Either
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.widgets.WidgetType

class WidgetHelper(private val cacheService: CacheService) {
    fun getWidgetIds(): Either<Failure, List<String>> {
        return cacheService.getWidgetIds()
    }

    fun getWidgetTypeById(appWidgetManager: AppWidgetManager, appWidgetId: Int): WidgetType {
        return when (appWidgetManager.getAppWidgetInfo(appWidgetId).initialLayout) {
            R.layout.widget_current_weather -> WidgetType.CURRENT_WEATHER
            R.layout.widget_current_weather_tile -> WidgetType.CURRENT_WEATHER_TILE
            else -> WidgetType.CURRENT_WEATHER
        }
    }
}
