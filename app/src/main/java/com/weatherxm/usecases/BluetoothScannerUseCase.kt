package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.ScannedDevice
import kotlinx.coroutines.flow.Flow

interface BluetoothScannerUseCase {
    suspend fun registerOnScanning(): Flow<ScannedDevice>
    suspend fun startScanning(): Flow<Either<Failure, Int>>
    fun stopScanning()
}
