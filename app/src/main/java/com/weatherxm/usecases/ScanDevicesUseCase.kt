package com.weatherxm.usecases

import com.weatherxm.data.repository.BluetoothRepository
import com.weatherxm.ui.ScannedDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

interface ScanDevicesUseCase {
    suspend fun scanBleDevices(): Flow<ScannedDevice>
}

class ScanDevicesUseCaseImpl(
    private val bluetoothRepository: BluetoothRepository,
) : ScanDevicesUseCase {

    override suspend fun scanBleDevices(): Flow<ScannedDevice> {
        return bluetoothRepository.scanBleDevices().map {
            Timber.d("New advertisement collected: $it")
            ScannedDevice(it.address, it.name)
        }
    }
}
