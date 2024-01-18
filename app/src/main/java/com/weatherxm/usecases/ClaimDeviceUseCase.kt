package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.CountryAndFrequencies
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.repository.AddressRepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.ui.common.UIDevice

interface ClaimDeviceUseCase {
    suspend fun claimDevice(
        serialNumber: String,
        lat: Double,
        lon: Double,
        secret: String? = null
    ): Either<Failure, UIDevice>

    suspend fun getCountryAndFrequencies(location: Location): CountryAndFrequencies
}

class ClaimDeviceUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val addressRepository: AddressRepository
) : ClaimDeviceUseCase {

    override suspend fun claimDevice(
        serialNumber: String,
        lat: Double,
        lon: Double,
        secret: String?
    ): Either<Failure, UIDevice> {
        return deviceRepository.claimDevice(serialNumber, Location(lat, lon), secret).map {
            it.toUIDevice()
        }
    }

    override suspend fun getCountryAndFrequencies(location: Location): CountryAndFrequencies {
        return addressRepository.getCountryAndFrequencies(location)
    }
}
