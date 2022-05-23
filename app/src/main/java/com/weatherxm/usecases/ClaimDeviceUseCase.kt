package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.DeviceRepository

interface ClaimDeviceUseCase {
    suspend fun claimDevice(serialNumber: String, lat: Double, lon: Double): Either<Failure, Device>
    suspend fun fetchUserEmail(): Either<Error, String>
}

class ClaimDeviceUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val authRepository: AuthRepository
) : ClaimDeviceUseCase {

    override suspend fun claimDevice(
        serialNumber: String,
        lat: Double,
        lon: Double
    ): Either<Failure, Device> {
        return deviceRepository.claimDevice(serialNumber, Location(lat, lon))
    }

    override suspend fun fetchUserEmail(): Either<Error, String> {
        return authRepository.isLoggedIn().map {
            it
        }
    }
}
