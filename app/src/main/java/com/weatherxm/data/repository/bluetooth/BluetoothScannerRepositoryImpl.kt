package com.weatherxm.data.repository.bluetooth

import arrow.core.Either
import com.weatherxm.data.BluetoothDeviceWithEUI
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.bluetooth.BluetoothScannerDataSource
import kotlinx.coroutines.flow.Flow

class BluetoothScannerRepositoryImpl(
    private val dataSource: BluetoothScannerDataSource
) : BluetoothScannerRepository {

    /*
    * Suppress this because we have asked for permissions already before we reach here.
    */
    @Suppress("MissingPermission")
    override suspend fun registerOnScanning(): Flow<BluetoothDeviceWithEUI> {
        return dataSource.registerOnScanning()
    }

    override suspend fun startScanning() {
        dataSource.startScanning()
    }

    override fun registerOnScanCompletionStatus(): Flow<Either<Failure, Unit>> {
        return dataSource.registerOnScanCompletionStatus()
    }

    override fun getScannedDevice(macAddress: String): BluetoothDeviceWithEUI? {
        return dataSource.getScannedDevice(macAddress)
    }
}
