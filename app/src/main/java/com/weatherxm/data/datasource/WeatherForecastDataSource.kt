package com.weatherxm.data.datasource

import androidx.annotation.StringDef
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData
import java.time.ZonedDateTime

interface WeatherForecastDataSource {

    companion object {
        const val EXCLUDE_DAILY = "daily"
        const val EXCLUDE_HOURLY = "hourly"
    }

    @Target(AnnotationTarget.TYPE)
    @StringDef(value = [EXCLUDE_DAILY, EXCLUDE_HOURLY])
    @Retention(AnnotationRetention.SOURCE)
    private annotation class Exclude

    suspend fun getForecast(
        deviceId: String,
        fromDate: ZonedDateTime,
        toDate: ZonedDateTime,
        exclude: @Exclude String? = null
    ): Either<Failure, List<WeatherData>>

    suspend fun setForecast(deviceId: String, forecast: List<WeatherData>)
    suspend fun clear()
}
