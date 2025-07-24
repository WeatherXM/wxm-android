package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.repository.WeatherForecastRepository
import com.weatherxm.ui.common.LocationWeather
import com.weatherxm.util.Weather
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

@Suppress("LongParameterList")
class LocationWeatherUseCaseImpl(
    private val weatherForecastRepository: WeatherForecastRepository
) : LocationWeatherUseCase {

    @Suppress("MagicNumber")
    override suspend fun getLocationWeather(
        lat: Double,
        lon: Double
    ): Either<Failure, LocationWeather> {
        return weatherForecastRepository.getLocationForecast(lat, lon).map { result ->
            val timezone = result.first().tz
            val nowInTimezone = ZonedDateTime.now(ZoneId.of(timezone))
            val todayHourlies = result.first().hourly

            /**
             * Find the closest hourly in the above regarding the nowInTimezone, e.g.
             * if it's 05:01 map it to 05:00, if it is 05:31 map it to 06:00, etc.
             *
             * Step 1: round nowInTimezone to the nearest hour
             * Step 2: find the HourlyWeather whose timestamp matches the rounded hour
             */
            val roundedHourToLookFor = if (nowInTimezone.minute >= 30) {
                nowInTimezone.plusHours(1).withMinute(0).withSecond(0).withNano(0)
            } else {
                nowInTimezone.withMinute(0).withSecond(0).withNano(0)
            }

            val closestHourly = todayHourlies?.minByOrNull {
                abs(it.timestamp.toEpochSecond() - roundedHourToLookFor.toEpochSecond())
            }

            LocationWeather(
                address = result[0].address,
                icon = closestHourly?.icon,
                currentWeatherSummaryResId = Weather.getWeatherSummaryDesc(closestHourly?.icon),
                currentTemp = closestHourly?.temperature,
                dailyMinTemp = result.first().daily?.temperatureMin,
                dailyMaxTemp = result.first().daily?.temperatureMax
            )
        }
    }
}
