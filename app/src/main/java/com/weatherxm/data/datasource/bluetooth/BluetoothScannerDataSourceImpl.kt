package com.weatherxm.data.datasource.bluetooth

import arrow.core.Either
import com.weatherxm.data.BluetoothDeviceWithEUI
import com.weatherxm.data.Failure
import com.weatherxm.data.bluetooth.BluetoothScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class BluetoothScannerDataSourceImpl(
    private val scanner: BluetoothScanner
) : BluetoothScannerDataSource {
    private val currentScannedDevices = mutableMapOf<String, BluetoothDeviceWithEUI>()

    override suspend fun registerOnScanning(): Flow<BluetoothDeviceWithEUI> {
        return scanner.registerOnScanning().onEach {
            currentScannedDevices[it.bluetoothDevice.address] = it
        }
    }

    override suspend fun startScanning() {
        currentScannedDevices.clear()
        scanner.startScanning()
    }

    override fun registerOnScanCompletionStatus(): Flow<Either<Failure, Unit>> {
        return scanner.getCompletionStatus()
    }
}
