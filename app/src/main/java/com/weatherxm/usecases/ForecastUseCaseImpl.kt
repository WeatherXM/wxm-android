package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.ApiError
import com.weatherxm.data.Failure
import com.weatherxm.data.network.ErrorResponse.Companion.INVALID_TIMEZONE
import com.weatherxm.data.repository.WeatherForecastRepository
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
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
    ): Either<Failure, List<UIForecast>> {
        if (device.timezone.isNullOrEmpty()) {
            return Either.Left(ApiError.UserError.InvalidTimezone(INVALID_TIMEZONE))
        }
        val dateStart = ZonedDateTime.now(ZoneId.of(device.timezone))
        val dateEnd = dateStart.plusDays(7)
        return weatherForecastRepository.getDeviceForecast(
            device.id,
            dateStart,
            dateEnd,
            forceRefresh
        ).map { result ->
            result.map {
                UIForecast(
                    it.date,
                    icon = it.daily?.icon,
                    maxTemp = it.daily?.temperatureMax,
                    minTemp = it.daily?.temperatureMin,
                    precipProbability = it.daily?.precipProbability,
                    precip = it.daily?.precipIntensity,
                    windSpeed = it.daily?.windSpeed,
                    windDirection = it.daily?.windDirection,
                    humidity = it.daily?.humidity,
                    hourlyWeather = it.hourly
                )
            }
        }
    }
}
