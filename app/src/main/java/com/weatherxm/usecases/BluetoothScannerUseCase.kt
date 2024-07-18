package com.weatherxm.usecases

import com.weatherxm.ui.common.ScannedDevice
import kotlinx.coroutines.flow.Flow

interface BluetoothScannerUseCase {
    suspend fun scan(): Flow<ScannedDevice>
}
