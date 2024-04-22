package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.ApiError
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.network.ErrorResponse.Companion.INVALID_TIMEZONE
import com.weatherxm.data.repository.WeatherForecastRepository
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.common.UIForecastDay
import com.weatherxm.util.isSameDayAndHour
import java.time.ZoneId
import java.time.ZonedDateTime

@Suppress("LongParameterList")
class ForecastUseCaseImpl(
    private val weatherForecastRepository: WeatherForecastRepository
) : ForecastUseCase {

    @Suppress("MagicNumber")
    override suspend fun getForecast(
        device: UIDevice,
        forceRefresh: Boolean
    ): Either<Failure, UIForecast> {
        if (device.timezone.isNullOrEmpty()) {
            return Either.Left(ApiError.UserError.InvalidTimezone(INVALID_TIMEZONE))
        }
        val nowDeviceTz = ZonedDateTime.now(ZoneId.of(device.timezone))
        val dateEndInDeviceTz = nowDeviceTz.plusDays(7)
        return weatherForecastRepository.getDeviceForecast(
            device.id,
            nowDeviceTz,
            dateEndInDeviceTz,
            forceRefresh
        ).map { result ->
            val nextHourlyWeatherForecast = mutableListOf<HourlyWeather>()
            val forecastDays = result.map { weatherData ->
                weatherData.hourly?.filter {
                    val isCurrentHour = it.timestamp.isSameDayAndHour(nowDeviceTz)
                    val isFutureHour = it.timestamp.isAfter(nowDeviceTz)
                    isCurrentHour || (isFutureHour && it.timestamp < nowDeviceTz.plusHours(24))
                }?.apply {
                    nextHourlyWeatherForecast.addAll(this)
                }

                UIForecastDay(
                    weatherData.date,
                    icon = weatherData.daily?.icon,
                    maxTemp = weatherData.daily?.temperatureMax,
                    minTemp = weatherData.daily?.temperatureMin,
                    precipProbability = weatherData.daily?.precipProbability,
                    precip = weatherData.daily?.precipIntensity,
                    windSpeed = weatherData.daily?.windSpeed,
                    windDirection = weatherData.daily?.windDirection,
                    humidity = weatherData.daily?.humidity,
                    pressure = weatherData.daily?.pressure,
                    uv = weatherData.daily?.uvIndex,
                    hourlyWeather = weatherData.hourly
                )
            }

            UIForecast(nextHourlyWeatherForecast, forecastDays)
        }
    }
}
