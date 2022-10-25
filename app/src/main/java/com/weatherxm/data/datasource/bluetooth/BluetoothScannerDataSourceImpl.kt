package com.weatherxm.data.datasource.bluetooth

import arrow.core.Either
import com.weatherxm.data.BluetoothDeviceWithEUI
import com.weatherxm.data.Failure
import com.weatherxm.data.bluetooth.BluetoothScanner
import kotlinx.coroutines.flow.Flow

class BluetoothScannerDataSourceImpl(
    private val scanner: BluetoothScanner
) : BluetoothScannerDataSource {
    override suspend fun registerOnScanning(): Flow<BluetoothDeviceWithEUI> {
        return scanner.registerOnScanning()
    }

    override suspend fun startScanning(): Either<Failure, Unit> {
        return scanner.startScanning()
    }
}
