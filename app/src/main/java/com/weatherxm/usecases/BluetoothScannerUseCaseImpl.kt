package com.weatherxm.usecases

import android.annotation.SuppressLint
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.bluetooth.BluetoothScannerRepository
import com.weatherxm.ui.common.ScannedDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class BluetoothScannerUseCaseImpl(
    private val bluetoothScannerRepository: BluetoothScannerRepository,
) : BluetoothScannerUseCase {

    /*
    * Suppress this because we have asked for permissions already before we reach here.
    */
    @SuppressLint("MissingPermission")
    override suspend fun registerOnScanning(): Flow<ScannedDevice> {
        return bluetoothScannerRepository.registerOnScanning().map {
            Timber.d("New bluetooth device collected: $it")
            ScannedDevice(address = it.address, name = it.name)
        }
    }

    override suspend fun startScanning(): Flow<Either<Failure, Int>> {
        return bluetoothScannerRepository.startScanning()
    }

    override fun stopScanning() {
        bluetoothScannerRepository.stopScanning()
    }
}
