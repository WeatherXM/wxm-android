package com.weatherxm.data.datasource.bluetooth

import com.juul.kable.Advertisement
import kotlinx.coroutines.flow.Flow

interface BluetoothScannerDataSource {
    suspend fun scan(): Flow<Advertisement>
}
