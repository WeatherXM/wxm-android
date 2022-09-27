package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.BluetoothRepository
import com.weatherxm.ui.ScannedDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

interface ScanDevicesUseCase {
    suspend fun registerOnScanning(): Flow<ScannedDevice>
    suspend fun registerOnScanningCompletionStatus(): Flow<Either<Failure, Unit>>
    suspend fun startScanning()
}

class ScanDevicesUseCaseImpl(
    private val bluetoothRepository: BluetoothRepository,
) : ScanDevicesUseCase {

    /*
    * Suppress this because we have asked for permissions already before we reach here.
    */
    @Suppress("MissingPermission")
    override suspend fun registerOnScanning(): Flow<ScannedDevice> {
        return bluetoothRepository.registerOnScanning().map {
            Timber.d("New bluetooth device collected: $it")
            ScannedDevice(
                address = it.address,
                name = it.name
            )
        }
    }

    override suspend fun registerOnScanningCompletionStatus(): Flow<Either<Failure, Unit>> {
        return bluetoothRepository.registerOnScanCompletionStatus()
    }

    override suspend fun startScanning() {
        bluetoothRepository.startScanning()
    }
}
