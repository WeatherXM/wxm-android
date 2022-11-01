package com.weatherxm.data.datasource.bluetooth

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.weatherxm.data.Failure
import kotlinx.coroutines.flow.Flow

interface BluetoothScannerDataSource {
    suspend fun registerOnScanning(): Flow<BluetoothDevice>
    suspend fun startScanning(): Flow<Either<Failure, Int>>
    fun stopScanning()
}
