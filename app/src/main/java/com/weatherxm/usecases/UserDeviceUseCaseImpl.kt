package com.weatherxm.usecases

import android.content.Context
import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceProfile.Helium
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.SharedPreferencesRepository
import com.weatherxm.data.repository.TokenRepository
import com.weatherxm.data.repository.WeatherForecastRepository
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.TokenInfo
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.common.UserDevice
import com.weatherxm.util.DateTimeHelper.getFormattedRelativeDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class UserDeviceUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val deviceOTARepository: DeviceOTARepository,
    private val tokenRepository: TokenRepository,
    private val weatherForecastRepository: WeatherForecastRepository,
    private val preferencesRepository: SharedPreferencesRepository,
    private val context: Context
) : UserDeviceUseCase {

    companion object {
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

    override suspend fun getUserDevices(): Either<Failure, List<UserDevice>> {
        return deviceRepository.getUserDevices().map {
            it.map { device ->
                val shouldShowOTAPrompt = deviceOTARepository.shouldShowOTAPrompt(
                    device.id,
                    device.attributes?.firmware?.assigned
                )
                val alerts = mutableListOf<DeviceAlert>()
                if (device.attributes?.isActive == false) {
                    alerts.add(DeviceAlert.OFFLINE)
                }

                if (shouldShowOTAPrompt && device.profile == Helium && device.needsUpdate()) {
                    alerts.add(DeviceAlert.NEEDS_UPDATE)
                }
                UserDevice(device, alerts)
            }
        }
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

    @Suppress("MagicNumber")
    override suspend fun getForecast(
        device: Device,
        forceRefresh: Boolean
    ): Either<Failure, List<UIForecast>> {
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
                    nameOfDayAndDate = it.date.getFormattedRelativeDay(context, true),
                    icon = it.daily?.icon,
                    maxTemp = it.daily?.temperatureMax,
                    minTemp = it.daily?.temperatureMin,
                    precipProbability = it.daily?.precipProbability,
                    windSpeed = it.daily?.windSpeed,
                    windDirection = it.daily?.windDirection,
                    humidity = it.daily?.humidity,
                    hourlyWeather = it.hourly
                )
            }
        }
    }
}
