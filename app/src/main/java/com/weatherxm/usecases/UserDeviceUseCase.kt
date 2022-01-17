package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.WeatherData
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.TokenRepository
import com.weatherxm.data.repository.WeatherRepository
import com.weatherxm.ui.userdevice.TokenSummary
import com.weatherxm.util.getFormattedDate
import com.weatherxm.util.getNowInTimezone
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface UserDeviceUseCase {
    suspend fun getUserDevices(): Either<Failure, List<Device>>
    suspend fun getTodayForecast(device: Device): Either<Failure, List<HourlyWeather>>
    suspend fun getTomorrowForecast(device: Device): Either<Failure, List<HourlyWeather>>
    suspend fun getTokensSummary24H(deviceId: String): Either<Failure, TokenSummary>
    suspend fun getTokensSummary7D(deviceId: String): Either<Failure, TokenSummary>
    suspend fun getTokensSummary30D(deviceId: String): Either<Failure, TokenSummary>
}

class UserDeviceUseCaseImpl : UserDeviceUseCase, KoinComponent {
    private val deviceRepository: DeviceRepository by inject()
    private val tokenRepository: TokenRepository by inject()
    private val weatherRepository: WeatherRepository by inject()

    override suspend fun getUserDevices(): Either<Failure, List<Device>> {
        return deviceRepository.getUserDevices()
    }

    override suspend fun getTokensSummary24H(deviceId: String): Either<Failure, TokenSummary> {
        return tokenRepository.getTokensSummary24H(deviceId).map {
            it.toTokenSummary()
        }
    }

    override suspend fun getTokensSummary7D(deviceId: String): Either<Failure, TokenSummary> {
        return tokenRepository.getTokensSummary7D(deviceId).map {
            it.toTokenSummary()
        }
    }

    override suspend fun getTokensSummary30D(deviceId: String): Either<Failure, TokenSummary> {
        return tokenRepository.getTokensSummary30D(deviceId).map {
            it.toTokenSummary()
        }
    }

    override suspend fun getTodayForecast(device: Device): Either<Failure, List<HourlyWeather>> {
        val now = getNowInTimezone(device.timezone)
        val today = getFormattedDate(now.toString())

        return weatherRepository.getHourlyForecast(device.id, today, today)
            .map {
                getHourlyWeatherFromData(it, today)
            }
    }

    override suspend fun getTomorrowForecast(device: Device): Either<Failure, List<HourlyWeather>> {
        val now = getNowInTimezone(device.timezone)
        val tomorrow = getFormattedDate(now.plusDays(1).toString())

        return weatherRepository.getHourlyForecast(device.id, tomorrow, tomorrow)
            .map {
                getHourlyWeatherFromData(it, tomorrow)
            }
    }

    private fun getHourlyWeatherFromData(
        weatherData: List<WeatherData>,
        date: String
    ): List<HourlyWeather> {
        var dataToReturn = listOf<HourlyWeather>()
        weatherData.forEach {
            if (it.date.equals(date) && it.hourly != null) {
                dataToReturn = it.hourly
            }
        }

        return dataToReturn
    }
}
