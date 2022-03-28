package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.WeatherData
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.TokenRepository
import com.weatherxm.data.repository.WeatherRepository
import com.weatherxm.ui.TokenSummary
import com.weatherxm.util.getFormattedDate
import com.weatherxm.util.getNowInTimezone
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface UserDeviceUseCase {
    suspend fun getUserDevices(): Either<Failure, List<Device>>
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    suspend fun getTodayForecast(device: Device): Either<Failure, List<HourlyWeather>>
    suspend fun getTomorrowForecast(device: Device): Either<Failure, List<HourlyWeather>>
    suspend fun getTokens24H(
        deviceId: String,
        forceRefresh: Boolean = false
    ): Either<Failure, Float?>

    suspend fun getTokens7D(
        deviceId: String,
        forceRefresh: Boolean = false
    ): Either<Failure, TokenSummary>

    suspend fun getTokens30D(
        deviceId: String,
        forceRefresh: Boolean = false
    ): Either<Failure, TokenSummary>
}

class UserDeviceUseCaseImpl : UserDeviceUseCase, KoinComponent {
    private val deviceRepository: DeviceRepository by inject()
    private val tokenRepository: TokenRepository by inject()
    private val weatherRepository: WeatherRepository by inject()

    override suspend fun getUserDevices(): Either<Failure, List<Device>> {
        return deviceRepository.getUserDevices()
    }

    override suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        return deviceRepository.getUserDevice(deviceId)
    }

    override suspend fun getTokens24H(
        deviceId: String,
        forceRefresh: Boolean
    ): Either<Failure, Float?> {
        return tokenRepository.getTokens24H(deviceId, forceRefresh)
    }

    override suspend fun getTokens7D(
        deviceId: String,
        forceRefresh: Boolean
    ): Either<Failure, TokenSummary> {
        return tokenRepository.getTokens7D(deviceId, forceRefresh).map {
            it.toTokenSummary()
        }
    }

    override suspend fun getTokens30D(
        deviceId: String,
        forceRefresh: Boolean
    ): Either<Failure, TokenSummary> {
        return tokenRepository.getTokens30D(deviceId, forceRefresh).map {
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
