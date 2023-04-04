package com.weatherxm.usecases

import android.location.Location
import arrow.core.Either
import com.weatherxm.data.CountryAndFrequencies
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceInfo
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.Failure
import com.weatherxm.data.Frequency
import com.weatherxm.data.UserActionError
import com.weatherxm.data.otherFrequencies
import com.weatherxm.data.repository.AddressRepository
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.DeviceRepository
import kotlinx.coroutines.runBlocking
import java.util.Date
import java.util.concurrent.TimeUnit

class StationSettingsUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val deviceOTARepository: DeviceOTARepository,
    private val addressRepository: AddressRepository
) : StationSettingsUseCase {

    companion object {
        // Allow device friendly name change once in 10 minutes
        val FRIENDLY_NAME_TIME_LIMIT = TimeUnit.MINUTES.toMillis(10)
    }

    override suspend fun setFriendlyName(
        deviceId: String, friendlyName: String
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

    override suspend fun removeDevice(serialNumber: String): Either<Failure, Unit> {
        return deviceRepository.removeDevice(serialNumber)
    }

    override fun shouldShowOTAPrompt(device: Device): Boolean {
        return deviceOTARepository.shouldShowOTAPrompt(
            device.id, device.attributes?.firmware?.assigned
        ) && device.profile?.equals(DeviceProfile.Helium) == true
    }

    override suspend fun getCountryAndFrequencies(
        lat: Double?,
        lon: Double?
    ): CountryAndFrequencies {
        if (lat == null || lon == null) {
            return CountryAndFrequencies(null, Frequency.US915, otherFrequencies(Frequency.US915))
        }
        val location = Location(null)
        location.longitude = lon
        location.latitude = lat
        return addressRepository.getCountryAndFrequencies(location)
    }

    override suspend fun getDeviceInfo(deviceId: String): Either<Failure, DeviceInfo> {
        return deviceRepository.getDeviceInfo(deviceId)
    }
}
