package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData
import com.weatherxm.data.repository.WeatherRepository
import com.weatherxm.ui.DailyForecast
import com.weatherxm.ui.ForecastData
import com.weatherxm.util.DateTimeHelper.getShortNameOfDayFromLocalDate
import com.weatherxm.util.DateTimeHelper.getSimplifiedDate
import com.weatherxm.util.ResourcesHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.ZonedDateTime

interface ForecastUseCase {
    suspend fun getDailyForecast(
        deviceId: String,
        fromDate: ZonedDateTime,
        toDate: ZonedDateTime
    ): Either<Failure, ForecastData>
}

class ForecastUseCaseImpl : ForecastUseCase, KoinComponent {
    private val weatherRepository: WeatherRepository by inject()
    private val resourcesHelper: ResourcesHelper by inject()

    override suspend fun getDailyForecast(
        deviceId: String,
        fromDate: ZonedDateTime,
        toDate: ZonedDateTime
    ): Either<Failure, ForecastData> {
        return weatherRepository.getDeviceForecast(deviceId, fromDate, toDate, false).map {
            val dailyForecasts = mutableListOf<DailyForecast>()
            var minTemp: Float? = null
            var maxTemp: Float? = null

            for (weatherData in it) {
                dailyForecasts.add(createDailyForecast(weatherData))

                val dayTempMin = weatherData.daily?.temperatureMin
                val dayTempMax = weatherData.daily?.temperatureMax

                if (minTemp == null && dayTempMin != null) {
                    minTemp = dayTempMin
                } else if (dayTempMin != null && minTemp != null && minTemp > dayTempMin) {
                    minTemp = dayTempMin
                }

                if (maxTemp == null && dayTempMax != null) {
                    maxTemp = dayTempMax
                } else if (dayTempMax != null && maxTemp != null && maxTemp < dayTempMax) {
                    maxTemp = dayTempMax
                }
            }

            return Either.Right(ForecastData(minTemp, maxTemp, dailyForecasts))
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
