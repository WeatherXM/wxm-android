package com.weatherxm.data.datasource

import androidx.annotation.StringDef
import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.WeatherData
import java.time.LocalDate

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
        fromDate: LocalDate,
        toDate: LocalDate,
        exclude: @Exclude String? = null
    ): Either<Failure, List<WeatherData>>

    suspend fun setForecast(deviceId: String, forecast: List<WeatherData>)
    suspend fun clear()
}
