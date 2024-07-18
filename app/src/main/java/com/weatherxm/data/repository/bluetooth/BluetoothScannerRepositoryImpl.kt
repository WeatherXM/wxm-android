package com.weatherxm.data.repository.bluetooth

import com.juul.kable.Advertisement
import com.weatherxm.data.datasource.bluetooth.BluetoothScannerDataSource
import kotlinx.coroutines.flow.Flow

class BluetoothScannerRepositoryImpl(
    private val dataSource: BluetoothScannerDataSource
) : BluetoothScannerRepository {

    override suspend fun scan(): Flow<Advertisement> {
        return dataSource.scan()
    }
}
