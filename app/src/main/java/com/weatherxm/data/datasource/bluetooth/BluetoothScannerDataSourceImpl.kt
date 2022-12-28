package com.weatherxm.data.datasource.bluetooth

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.bluetooth.BluetoothScanner
import kotlinx.coroutines.flow.Flow

class BluetoothScannerDataSourceImpl(
    private val scanner: BluetoothScanner
) : BluetoothScannerDataSource {
    override suspend fun registerOnScanning(): Flow<BluetoothDevice> {
        return scanner.registerOnScanning()
    }

    override suspend fun startScanning(): Flow<Either<Failure, Int>> {
        return scanner.startScanning()
    }

    override fun stopScanning() {
        scanner.stopScanning()
    }
}
