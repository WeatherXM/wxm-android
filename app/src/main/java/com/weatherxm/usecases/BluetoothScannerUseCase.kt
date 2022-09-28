package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.bluetooth.BluetoothScannerRepository
import com.weatherxm.ui.ScannedDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

interface BluetoothScannerUseCase {
    suspend fun registerOnScanning(): Flow<ScannedDevice>
    suspend fun registerOnScanningCompletionStatus(): Flow<Either<Failure, Unit>>
    suspend fun startScanning()
}

class BluetoothScannerUseCaseImpl(
    private val bluetoothScannerRepository: BluetoothScannerRepository,
) : BluetoothScannerUseCase {

    /*
    * Suppress this because we have asked for permissions already before we reach here.
    */
    @Suppress("MissingPermission")
    override suspend fun registerOnScanning(): Flow<ScannedDevice> {
        return bluetoothScannerRepository.registerOnScanning().map {
            Timber.d("New bluetooth device collected: $it")
            ScannedDevice(
                address = it.bluetoothDevice.address,
                name = it.bluetoothDevice.name,
                eui = it.devEUI
            )
        }
    }

    override suspend fun registerOnScanningCompletionStatus(): Flow<Either<Failure, Unit>> {
        return bluetoothScannerRepository.registerOnScanCompletionStatus()
    }

    override suspend fun startScanning() {
        bluetoothScannerRepository.startScanning()
    }
}
