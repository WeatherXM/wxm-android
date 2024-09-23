package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.datasource.WidgetDataSource

interface WidgetRepository {
    fun getWidgetDevice(widgetId: Int): Either<Failure, String>
    fun setWidgetDevice(widgetId: Int, deviceId: String)
    fun setWidgetId(widgetId: Int)
    fun removeWidgetId(widgetId: Int)
}

class WidgetRepositoryImpl(
    private val dataSource: WidgetDataSource
) : WidgetRepository {

    override fun getWidgetDevice(widgetId: Int): Either<Failure, String> {
        return dataSource.getWidgetDevice(widgetId)
    }

    override fun setWidgetDevice(widgetId: Int, deviceId: String) {
        dataSource.setWidgetDevice(widgetId, deviceId)
    }

    override fun setWidgetId(widgetId: Int) {
        dataSource.setWidgetId(widgetId)
    }

    override fun removeWidgetId(widgetId: Int) {
        dataSource.removeWidgetId(widgetId)
    }
}
