package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData
import com.weatherxm.data.repository.WeatherRepository
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
    private val weatherRepository: WeatherRepository,
    private val resourcesHelper: ResourcesHelper
) : ForecastUseCase {

    override suspend fun getDailyForecast(
        deviceId: String,
        fromDate: ZonedDateTime,
        toDate: ZonedDateTime,
        forceRefresh: Boolean
    ): Either<Failure, List<DailyForecast>> {
        return weatherRepository.getDeviceForecast(deviceId, fromDate, toDate, forceRefresh).map {
            val dailyForecasts = mutableListOf<DailyForecast>()

            for (weatherData in it) {
                dailyForecasts.add(createDailyForecast(weatherData))
            }

            return Either.Right(dailyForecasts)
        }
    }

    private fun createDailyForecast(weatherData: WeatherData): DailyForecast {
        val dailyForecast = DailyForecast()

        weatherData.date?.let {
            dailyForecast.nameOfDay = getShortNameOfDayFromLocalDate(resourcesHelper, it)
            dailyForecast.dateOfDay = getSimplifiedDate(it)
        }

        dailyForecast.icon = weatherData.daily?.icon
        dailyForecast.maxTemp = weatherData.daily?.temperatureMax
        dailyForecast.minTemp = weatherData.daily?.temperatureMin
        dailyForecast.precipProbability = weatherData.daily?.precipProbability

        return dailyForecast
    }
}
