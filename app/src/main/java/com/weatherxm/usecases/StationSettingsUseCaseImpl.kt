package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.CountryAndFrequencies
import com.weatherxm.data.DeviceInfo
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.Failure
import com.weatherxm.data.Frequency
import com.weatherxm.data.Location
import com.weatherxm.data.otherFrequencies
import com.weatherxm.data.repository.AddressRepository
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.ui.common.UIDevice

class StationSettingsUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val deviceOTARepository: DeviceOTARepository,
    private val addressRepository: AddressRepository
) : StationSettingsUseCase {

    override suspend fun setFriendlyName(
        deviceId: String, friendlyName: String
    ): Either<Failure, Unit> {
        return deviceRepository.setFriendlyName(deviceId, friendlyName)
    }

    override suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit> {
        return deviceRepository.clearFriendlyName(deviceId)
    }

    override suspend fun removeDevice(serialNumber: String, id: String): Either<Failure, Unit> {
        return deviceRepository.removeDevice(serialNumber, id)
    }

    override fun shouldShowOTAPrompt(device: UIDevice): Boolean {
        return deviceOTARepository.shouldShowOTAPrompt(device.id, device.assignedFirmware)
            && device.profile?.equals(DeviceProfile.Helium) == true
    }

    override suspend fun getCountryAndFrequencies(
        lat: Double?,
        lon: Double?
    ): CountryAndFrequencies {
        if (lat == null || lon == null) {
            return CountryAndFrequencies(null, Frequency.US915, otherFrequencies(Frequency.US915))
        }
        return addressRepository.getCountryAndFrequencies(Location(lat, lon))
    }

    override suspend fun getDeviceInfo(deviceId: String): Either<Failure, DeviceInfo> {
        return deviceRepository.getDeviceInfo(deviceId)
    }
}
