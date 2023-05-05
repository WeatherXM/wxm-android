package com.weatherxm.util

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.services.CacheService

class WidgetHelper(private val cacheService: CacheService) {
    fun getWidgetIds(): Either<Failure, List<String>> {
        return cacheService.getWidgetIds()
    }

    fun getWidgetsOfType(widgetIds: IntArray?, prefix: String): List<Int> {
        return widgetIds?.filter {
            cacheService.hasWidgetOfType(it, prefix)
        } ?: listOf()
    }

    fun setWidgetOfType(widgetId: Int, prefix: String, enabled: Boolean = true) {
        cacheService.setWidgetOfType(widgetId, prefix, enabled)
    }
}
