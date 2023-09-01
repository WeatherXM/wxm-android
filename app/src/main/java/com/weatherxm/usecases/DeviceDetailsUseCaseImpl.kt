package com.weatherxm.usecases

import android.content.Context
import arrow.core.Either
import com.weatherxm.data.DeviceProfile.Helium
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.AddressRepository
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.ExplorerRepository
import com.weatherxm.data.repository.SharedPreferencesRepository
import com.weatherxm.data.repository.TokenRepository
import com.weatherxm.data.repository.WeatherForecastRepository
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.RewardsInfo
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.util.DateTimeHelper.getFormattedRelativeDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@Suppress("LongParameterList")
class DeviceDetailsUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val deviceOTARepository: DeviceOTARepository,
    private val tokenRepository: TokenRepository,
    private val weatherForecastRepository: WeatherForecastRepository,
    private val addressRepository: AddressRepository,
    private val preferencesRepository: SharedPreferencesRepository,
    private val explorerRepository: ExplorerRepository,
    private val context: Context
) : DeviceDetailsUseCase {

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

    override suspend fun getUserDevices(): Either<Failure, List<UIDevice>> {
        return deviceRepository.getUserDevices().map { devices ->
            devices.map {
                val device = it.toUIDevice()
                val shouldShowOTAPrompt = deviceOTARepository.shouldShowOTAPrompt(
                    device.id,
                    device.assignedFirmware
                ) && device.relation == DeviceRelation.OWNED
                val alerts = mutableListOf<DeviceAlert>()
                if (device.isActive == false) {
                    alerts.add(DeviceAlert.OFFLINE)
                }

                if (shouldShowOTAPrompt && device.profile == Helium && device.needsUpdate()) {
                    alerts.add(DeviceAlert.NEEDS_UPDATE)
                }
                device.apply {
                    this.alerts = alerts
                }
            }
        }
    }

    override suspend fun getUserDevice(device: UIDevice): Either<Failure, UIDevice> {
        return if (device.relation == DeviceRelation.UNFOLLOWED) {
            explorerRepository.getCellDevice(device.cellIndex, device.id).map {
                it.toUIDevice().apply {
                    this.relation = DeviceRelation.UNFOLLOWED
                }
            }
        } else {
            deviceRepository.getUserDevice(device.id).map {
                it.toUIDevice()
            }
        }
    }

    // We suppress magic number because we use specific numbers to check last month and last week
    @Suppress("MagicNumber")
    override suspend fun getTokenInfoLast30D(device: UIDevice): Either<Failure, RewardsInfo> {
        // Last 29 days of transactions + today = 30 days
        val fromDate = ZonedDateTime.now().minusDays(29).toLocalDate().toString()

        return tokenRepository.getTransactionsInRange(
            deviceId = device.id,
            fromDate = fromDate
        ).map {
            RewardsInfo().fromLastAndDatedTxs(it)
        }
    }

    @Suppress("MagicNumber")
    override suspend fun getForecast(
        device: UIDevice,
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

    override suspend fun getAddressOfCell(cell: UICell): String? {
        return addressRepository.getAddressFromLocation(cell.index, cell.center)
    }
}
