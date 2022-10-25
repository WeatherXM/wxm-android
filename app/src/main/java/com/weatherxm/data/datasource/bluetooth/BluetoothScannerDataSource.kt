package com.weatherxm.data.datasource.bluetooth

import arrow.core.Either
import com.weatherxm.data.BluetoothDeviceWithEUI
import com.weatherxm.data.Failure
import kotlinx.coroutines.flow.Flow

interface BluetoothScannerDataSource {
    suspend fun registerOnScanning(): Flow<BluetoothDeviceWithEUI>
    suspend fun startScanning(): Either<Failure, Unit>
}
