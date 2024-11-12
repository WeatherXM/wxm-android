package com.weatherxm.usecases

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.juul.kable.Advertisement
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Frequency
import com.weatherxm.data.repository.bluetooth.BluetoothConnectionRepository
import kotlinx.coroutines.flow.Flow

class BluetoothConnectionUseCaseImpl(
    private val bluetoothConnectionRepository: BluetoothConnectionRepository
) : BluetoothConnectionUseCase {

    override fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothConnectionRepository.getPairedDevices()
    }

    override suspend fun setPeripheral(
        advertisement: Advertisement,
        address: String
    ): Either<Failure, Unit> {
        return bluetoothConnectionRepository.setPeripheral(advertisement, address)
    }

    override suspend fun connectToPeripheral(): Either<Failure, Unit> {
        return bluetoothConnectionRepository.connectToPeripheral()
    }

    override suspend fun disconnectFromPeripheral() {
        bluetoothConnectionRepository.disconnectFromPeripheral()
    }

    override fun registerOnBondStatus(): Flow<Int> {
        return bluetoothConnectionRepository.registerOnBondStatus()
    }

    override suspend fun fetchClaimingKey(): Either<Failure, String> {
        return bluetoothConnectionRepository.fetchClaimingKey()
    }

    override suspend fun fetchDeviceEUI(): Either<Failure, String> {
        return bluetoothConnectionRepository.fetchDeviceEUI()
    }

    override suspend fun setFrequency(frequency: Frequency): Either<Failure, Unit> {
        return bluetoothConnectionRepository.setFrequency(frequency)
    }

    override suspend fun reboot(): Either<Failure, Unit> {
        return bluetoothConnectionRepository.reboot()
    }
}
