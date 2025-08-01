package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.CountryAndFrequencies
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.repository.GeoLocationRepository
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
    private val geoLocationRepository: GeoLocationRepository
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
        return geoLocationRepository.getCountryAndFrequencies(location)
    }
}
