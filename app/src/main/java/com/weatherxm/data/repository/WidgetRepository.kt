package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.WidgetDataSource

interface WidgetRepository {
    fun getDeviceOfWidget(widgetId: Int): Either<Failure, String>
    fun setDeviceOfWidget(widgetId: Int, deviceId: String)
    fun setWidgetIds(widgetId: Int)
    fun removeWidgetId(widgetId: Int)
}

class WidgetRepositoryImpl(
    private val dataSource: WidgetDataSource
) : WidgetRepository {

    override fun getDeviceOfWidget(widgetId: Int): Either<Failure, String> {
        return dataSource.getDeviceOfWidget(widgetId)
    }

    override fun setDeviceOfWidget(widgetId: Int, deviceId: String) {
        dataSource.setDeviceOfWidget(widgetId, deviceId)
    }

    override fun setWidgetIds(widgetId: Int) {
        dataSource.setWidgetIds(widgetId.toString())
    }

    override fun removeWidgetId(widgetId: Int) {
        dataSource.removeWidgetId(widgetId.toString())
    }
}
