package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.services.CacheService

interface WidgetDataSource {
    fun getDeviceOfWidget(widgetId: Int): Either<Failure, String>
    fun setDeviceOfWidget(widgetId: Int, deviceId: String)
    fun setWidgetIds(widgetId: String)
    fun removeWidgetId(widgetId: String)
}

class WidgetDataSourceImpl(
    private val cacheService: CacheService
) : WidgetDataSource {

    override fun getDeviceOfWidget(widgetId: Int): Either<Failure, String> {
        val key = cacheService.getWidgetFormattedKey(widgetId)
        return cacheService.getDeviceOfWidget(key)
    }

    override fun setDeviceOfWidget(widgetId: Int, deviceId: String) {
        val key = cacheService.getWidgetFormattedKey(widgetId)
        cacheService.setDeviceOfWidget(key, deviceId)
    }

    override fun setWidgetIds(widgetId: String) {
        val currentWidgetIds = cacheService.getWidgetIds().fold({
            mutableListOf()
        }) {
            it.toMutableList()
        }
        currentWidgetIds.add(widgetId)
        cacheService.setWidgetIds(currentWidgetIds)
    }

    override fun removeWidgetId(widgetId: String) {
        cacheService.removeDeviceOfWidget(cacheService.getWidgetFormattedKey(widgetId.toInt()))

        val currentWidgetIds = cacheService.getWidgetIds().fold({
            mutableListOf()
        }) {
            it.toMutableList()
        }
        if (currentWidgetIds.contains(widgetId)) {
            currentWidgetIds.remove(widgetId)
        }
        cacheService.setWidgetIds(currentWidgetIds)
    }
}
