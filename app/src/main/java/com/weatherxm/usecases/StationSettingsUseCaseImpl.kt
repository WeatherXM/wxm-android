package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.CountryAndFrequencies
import com.weatherxm.data.models.DeviceInfo
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.repository.AddressRepository
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.ui.common.UIDevice

class StationSettingsUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val deviceOTARepository: DeviceOTARepository,
    private val addressRepository: AddressRepository,
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

    override fun shouldNotifyOTA(device: UIDevice): Boolean {
        return deviceOTARepository.shouldNotifyOTA(device.id, device.assignedFirmware)
    }

    override suspend fun getCountryAndFrequencies(
        lat: Double?,
        lon: Double?
    ): CountryAndFrequencies {
        if (lat == null || lon == null) {
            return CountryAndFrequencies.default()
        }
        return addressRepository.getCountryAndFrequencies(Location(lat, lon))
    }

    override suspend fun getDeviceInfo(deviceId: String): Either<Failure, DeviceInfo> {
        return deviceRepository.getDeviceInfo(deviceId)
    }
}
