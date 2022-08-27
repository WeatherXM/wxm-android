package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.WeatherForecastRepository
import com.weatherxm.ui.DailyForecast
import com.weatherxm.util.DateTimeHelper.getShortNameOfDayFromLocalDate
import com.weatherxm.util.DateTimeHelper.getSimplifiedDate
import com.weatherxm.util.ResourcesHelper
import java.time.ZonedDateTime

interface ForecastUseCase {
    suspend fun getDailyForecast(
        deviceId: String,
        fromDate: ZonedDateTime,
        toDate: ZonedDateTime,
        forceRefresh: Boolean = false
    ): Either<Failure, List<DailyForecast>>
}

class ForecastUseCaseImpl(
    private val weatherForecastRepository: WeatherForecastRepository,
    private val resourcesHelper: ResourcesHelper
) : ForecastUseCase {

    override suspend fun getDailyForecast(
        deviceId: String,
        fromDate: ZonedDateTime,
        toDate: ZonedDateTime,
        forceRefresh: Boolean
    ): Either<Failure, List<DailyForecast>> {
        return weatherForecastRepository.getDeviceForecast(deviceId, fromDate, toDate, forceRefresh)
            .map {
                it.map { weatherData ->
                    DailyForecast().apply {
                        weatherData.date?.let { date ->
                            nameOfDay = getShortNameOfDayFromLocalDate(resourcesHelper, date)
                            dateOfDay = getSimplifiedDate(date)
                        }
                        icon = weatherData.daily?.icon
                        maxTemp = weatherData.daily?.temperatureMax
                        minTemp = weatherData.daily?.temperatureMin
                        precipProbability = weatherData.daily?.precipProbability
                    }
                }
            }
    }
}
