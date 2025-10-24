package com.weatherxm.data.datasource

import arrow.core.Either
import arrow.core.handleErrorWith
import com.weatherxm.data.mapResponse
import com.weatherxm.data.models.ApiError.UserError.ClaimError.DeviceClaiming
import com.weatherxm.data.models.Device
import com.weatherxm.data.models.DeviceInfo
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ClaimDeviceBody
import com.weatherxm.data.network.DeleteDeviceBody
import com.weatherxm.data.network.FriendlyNameBody
import com.weatherxm.data.network.LocationBody
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.TimeUnit

class NetworkDeviceDataSource(private val apiService: ApiService) : DeviceDataSource {
    companion object {
        // 2.5 minutes in total
        const val CLAIM_MAX_RETRIES = 30
        val CLAIM_RETRY_DELAY = TimeUnit.SECONDS.toMillis(5L)
    }

    override suspend fun getUserDevices(): Either<Failure, List<Device>> {
        return apiService.getUserDevices().mapResponse()
    }

    override suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        return apiService.getUserDevice(deviceId).mapResponse()
    }

    override suspend fun claimDevice(
        serialNumber: String,
        location: Location,
        secret: String?,
        numOfRetries: Int
    ): Either<Failure, Device> {
        return apiService.claimDevice(ClaimDeviceBody(serialNumber, location, secret))
            .mapResponse()
            .handleErrorWith {
                if (it is DeviceClaiming && numOfRetries < CLAIM_MAX_RETRIES) {
                    Timber.d("Claiming Failed with ${it.code}. Retrying after 5 seconds...")
                    delay(CLAIM_RETRY_DELAY)
                    claimDevice(serialNumber, location, secret, numOfRetries + 1)
                } else {
                    Either.Left(it)
                }
            }
    }

    override suspend fun setFriendlyName(
        deviceId: String,
        friendlyName: String
    ): Either<Failure, Unit> {
        return apiService.setFriendlyName(deviceId, FriendlyNameBody(friendlyName)).mapResponse()
    }

    override suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit> {
        return apiService.clearFriendlyName(deviceId).mapResponse()
    }

    override suspend fun removeDevice(serialNumber: String): Either<Failure, Unit> {
        return apiService.removeDevice(DeleteDeviceBody(serialNumber)).mapResponse()
    }

    override suspend fun getDeviceInfo(deviceId: String): Either<Failure, DeviceInfo> {
        return apiService.getUserDeviceInfo(deviceId).mapResponse()
    }

    override suspend fun getUserDevicesFromCache(): List<Device> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun setUserDevices(devices: List<Device>) {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun setLocation(
        deviceId: String,
        lat: Double,
        lon: Double
    ): Either<Failure, Device> {
        return apiService.setLocation(deviceId, LocationBody(lat, lon)).mapResponse()
    }

    override suspend fun getDeviceHealthCheck(deviceName: String): String? {
        return apiService.getDeviceHealthCheck(deviceName).mapResponse().getOrNull()?.let {
            if (it.error != null) {
                Timber.e(it.error)
                null
            } else {
                it.outputs?.result
            }
        }
    }
}
