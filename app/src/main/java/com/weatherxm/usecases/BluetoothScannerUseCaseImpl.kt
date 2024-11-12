package com.weatherxm.usecases

import com.weatherxm.data.repository.bluetooth.BluetoothScannerRepository
import com.weatherxm.ui.common.ScannedDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BluetoothScannerUseCaseImpl(
    private val repository: BluetoothScannerRepository
) : BluetoothScannerUseCase {

    override suspend fun scan(): Flow<ScannedDevice> {
        return repository.scan().map {
            ScannedDevice(advertisement = it, address = it.identifier, name = it.name)
        }
    }
}
