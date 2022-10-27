package com.weatherxm.data.repository.bluetooth

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.weatherxm.data.Failure
import kotlinx.coroutines.flow.Flow

interface BluetoothScannerRepository {
    suspend fun registerOnScanning(): Flow<BluetoothDevice>
    suspend fun startScanning(): Either<Failure, Unit>
    fun stopScanning()
}
