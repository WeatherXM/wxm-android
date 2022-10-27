package com.weatherxm.data.repository.bluetooth

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.bluetooth.BluetoothScannerDataSource
import kotlinx.coroutines.flow.Flow

class BluetoothScannerRepositoryImpl(
    private val dataSource: BluetoothScannerDataSource
) : BluetoothScannerRepository {

    override suspend fun registerOnScanning(): Flow<BluetoothDevice> {
        return dataSource.registerOnScanning()
    }

    override suspend fun startScanning(): Either<Failure, Unit> {
        return dataSource.startScanning()
    }

    override fun stopScanning() {
        dataSource.stopScanning()
    }
}
