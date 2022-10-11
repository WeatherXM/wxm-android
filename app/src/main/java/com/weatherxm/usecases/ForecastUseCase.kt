package com.weatherxm.usecases

import android.content.Context
import arrow.core.Either
import com.weatherxm.data.DATE_FORMAT_MONTH_DAY
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.WeatherForecastRepository
import com.weatherxm.ui.deviceforecast.DailyForecast
import com.weatherxm.util.getShortName
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

interface ForecastUseCase {
    suspend fun getDailyForecast(
        deviceId: String,
        fromDate: ZonedDateTime,
        toDate: ZonedDateTime,
        forceRefresh: Boolean = false
    ): Either<Failure, List<DailyForecast>>
}

class ForecastUseCaseImpl(
    private val context: Context,
    private val weatherForecastRepository: WeatherForecastRepository
) : ForecastUseCase, KoinComponent {

    private val formatterMonthDay: DateTimeFormatter by inject(named(DATE_FORMAT_MONTH_DAY))

    override suspend fun getDailyForecast(
        deviceId: String,
        fromDate: ZonedDateTime,
        toDate: ZonedDateTime,
        forceRefresh: Boolean
    ): Either<Failure, List<DailyForecast>> {
        return weatherForecastRepository.getDeviceForecast(deviceId, fromDate, toDate, forceRefresh)
            .map { result ->
                result.map {
                    DailyForecast(
                        nameOfDay = it.date.format(formatterMonthDay),
                        dateOfDay = it.date.dayOfWeek.getShortName(context),
                        icon = it.daily?.icon,
                        maxTemp = it.daily?.temperatureMax,
                        minTemp = it.daily?.temperatureMin,
                        precipProbability = it.daily?.precipProbability
                    )
                }
            }
    }
}
