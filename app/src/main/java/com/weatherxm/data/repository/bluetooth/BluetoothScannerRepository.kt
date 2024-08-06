package com.weatherxm.data.repository.bluetooth

import com.juul.kable.Advertisement
import kotlinx.coroutines.flow.Flow

interface BluetoothScannerRepository {
    suspend fun scan(): Flow<Advertisement>
}
