package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.UserActionError
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.SharedPreferencesRepository
import com.weatherxm.data.repository.TokenRepository
import com.weatherxm.data.repository.WeatherForecastRepository
import com.weatherxm.ui.common.TokenInfo
import com.weatherxm.util.isToday
import com.weatherxm.util.isTomorrow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.runBlocking
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class UserDeviceUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val tokenRepository: TokenRepository,
    private val weatherForecastRepository: WeatherForecastRepository,
    private val preferencesRepository: SharedPreferencesRepository
) : UserDeviceUseCase {

    companion object {
        // Allow device friendly name change once in 10 minutes
        val FRIENDLY_NAME_TIME_LIMIT = TimeUnit.MINUTES.toMillis(10)

        val UNIT_PREF_KEYS = arrayOf(
            "temperature_unit",
            "precipitation_unit",
            "wind_speed_unit",
            "wind_direction_unit",
            "pressure_unit"
        )
    }

    override fun getUnitPreferenceChangedFlow(): Flow<String> {
        return preferencesRepository.getPreferenceChangeFlow()
            .filter { key -> key in UNIT_PREF_KEYS }
    }

    override suspend fun getUserDevices(): Either<Failure, List<Device>> {
        return deviceRepository.getUserDevices()
    }

    override suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        return deviceRepository.getUserDevice(deviceId)
    }

    // We suppress magic number because we use specific numbers to check last month and last week
    @Suppress("MagicNumber")
    override suspend fun getTokenInfoLast30D(deviceId: String): Either<Failure, TokenInfo> {
        // Last 29 days of transactions + today = 30 days
        val fromDate = ZonedDateTime.now().minusDays(29).toLocalDate().toString()
        return tokenRepository.getAllTransactionsInRange(
            deviceId = deviceId,
            fromDate = fromDate
        ).map {
            TokenInfo().fromLastAndDatedTxs(it)
        }
    }

    override suspend fun getTodayAndTomorrowForecast(
        device: Device,
        forceRefresh: Boolean
    ): Either<Failure, List<HourlyWeather>> {
        val dateStart = ZonedDateTime.now(ZoneId.of(device.timezone))
        val dateEnd = dateStart.plusDays(1)
        return weatherForecastRepository.getDeviceForecast(
            device.id,
            dateStart,
            dateEnd,
            forceRefresh
        ).map { response ->
            response
                .filter { it.date.isToday() || it.date.isTomorrow() }
                .mapNotNull { it.hourly }
                .flatten()
        }
    }

    override suspend fun setFriendlyName(
        deviceId: String,
        friendlyName: String
    ): Either<Failure, Unit> {
        return deviceRepository.setFriendlyName(deviceId, friendlyName)
    }

    override suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit> {
        return deviceRepository.clearFriendlyName(deviceId)
    }

    override fun canChangeFriendlyName(deviceId: String): Either<UserActionError, Boolean> {
        // Check if user has already set a friendly name within a predefined time window
        val lastFriendlyNameChanged = runBlocking {
            deviceRepository.getLastFriendlyNameChanged(deviceId)
        }
        val diff = Date().time - lastFriendlyNameChanged
        return if (diff >= FRIENDLY_NAME_TIME_LIMIT) {
            Either.Right(true)
        } else {
            Either.Left(
                UserActionError.UserActionRateLimitedError(
                    "${diff}ms passed since last name change [Limit ${FRIENDLY_NAME_TIME_LIMIT}ms]"
                )
            )
        }
    }
}
