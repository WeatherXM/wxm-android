package com.weatherxm.data.datasource.bluetooth

import com.juul.kable.Advertisement
import com.weatherxm.data.bluetooth.BluetoothScanner
import kotlinx.coroutines.flow.Flow

class BluetoothScannerDataSourceImpl(
    private val scanner: BluetoothScanner
) : BluetoothScannerDataSource {

    override suspend fun scan(): Flow<Advertisement> {
        return scanner.scan()
    }
}
