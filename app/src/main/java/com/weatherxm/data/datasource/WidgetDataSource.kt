package com.weatherxm.data.datasource

import arrow.core.Either
import arrow.core.getOrElse
import com.weatherxm.data.models.Failure
import com.weatherxm.data.services.CacheService

interface WidgetDataSource {
    fun getWidgetDevice(widgetId: Int): Either<Failure, String>
    fun setWidgetDevice(widgetId: Int, deviceId: String)
    fun setWidgetId(widgetId: Int)
    fun removeWidgetId(widgetId: Int)
}

class WidgetDataSourceImpl(
    private val cacheService: CacheService
) : WidgetDataSource {

    override fun getWidgetDevice(widgetId: Int): Either<Failure, String> {
        val key = CacheService.getWidgetFormattedKey(widgetId)
        return cacheService.getWidgetDevice(key)
    }

    override fun setWidgetDevice(widgetId: Int, deviceId: String) {
        val key = CacheService.getWidgetFormattedKey(widgetId)
        cacheService.setWidgetDevice(key, deviceId)
    }

    override fun setWidgetId(widgetId: Int) {
        val currentWidgetIds = cacheService.getWidgetIds().getOrElse {
            mutableListOf()
        }.toMutableList()
        currentWidgetIds.add(widgetId.toString())
        cacheService.setWidgetIds(currentWidgetIds)
    }

    override fun removeWidgetId(widgetId: Int) {
        cacheService.removeDeviceOfWidget(CacheService.getWidgetFormattedKey(widgetId))

        val currentWidgetIds = cacheService.getWidgetIds().getOrElse {
            mutableListOf()
        }.toMutableList()
        if (currentWidgetIds.contains(widgetId.toString())) {
            currentWidgetIds.remove(widgetId.toString())
        }
        cacheService.setWidgetIds(currentWidgetIds)
    }
}
