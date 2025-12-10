package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.WeatherData
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
    private val repo: WeatherForecastRepository
) : ForecastUseCase {

    @Suppress("MagicNumber")
    override suspend fun getDeviceForecast(
        device: UIDevice,
        forceRefresh: Boolean
    ): Either<Failure, UIForecast> {
        if (device.timezone.isNullOrEmpty()) {
            return Either.Left(ApiError.UserError.InvalidTimezone(INVALID_TIMEZONE))
        }
        val nowDeviceTz = ZonedDateTime.now(ZoneId.of(device.timezone))
        val dateEndInDeviceTz = nowDeviceTz.plusDays(7).toLocalDate()
        return repo.getDeviceForecast(
            device.id,
            nowDeviceTz.toLocalDate(),
            dateEndInDeviceTz,
            forceRefresh
        ).map {
            getUIForecastFromWeatherData(nowDeviceTz, it)
        }
    }

    override suspend fun getLocationForecast(location: Location): Either<Failure, UIForecast> {
        return repo.getLocationForecast(location).map {
            val timezone = it.first().tz
            val nowInTimezone = ZonedDateTime.now(ZoneId.of(timezone))
            getUIForecastFromWeatherData(nowInTimezone, it)
        }
    }

    @Suppress("MagicNumber")
    private fun getUIForecastFromWeatherData(
        nowInTimezone: ZonedDateTime,
        data: List<WeatherData>
    ): UIForecast {
        val nextHourlyWeatherForecast = mutableListOf<HourlyWeather>()
        val forecastDays = data.map { weatherData ->
            weatherData.hourly?.filter {
                val isCurrentHour = it.timestamp.isSameDayAndHour(nowInTimezone)
                val isFutureHour = it.timestamp.isAfter(nowInTimezone)
                isCurrentHour || (isFutureHour && it.timestamp < nowInTimezone.plusHours(24))
            }?.apply {
                nextHourlyWeatherForecast.addAll(this)
            }

            UIForecastDay(
                date = weatherData.date,
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

        return UIForecast(
            address = data[0].address,
            isPremium = data[0].isPremium,
            next24Hours = nextHourlyWeatherForecast,
            forecastDays = forecastDays
        )
    }
}
