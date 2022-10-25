package com.weatherxm.usecases

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.bluetooth.BluetoothConnectionRepository
import com.weatherxm.data.repository.bluetooth.BluetoothScannerRepository
import kotlinx.coroutines.flow.Flow

class BluetoothConnectionUseCaseImpl(
    private val bluetoothConnectionRepository: BluetoothConnectionRepository,
    private val bluetoothScannerRepository: BluetoothScannerRepository,
) : BluetoothConnectionUseCase {

    override fun getPairedDevices(): List<BluetoothDevice>? {
        return bluetoothConnectionRepository.getPairedDevices()
    }

    override fun setPeripheral(address: String): Either<Failure, Unit> {
        return bluetoothConnectionRepository.setPeripheral(address)
    }

    override suspend fun connectToPeripheral(): Either<Failure, Unit> {
        return bluetoothConnectionRepository.connectToPeripheral()
    }

    override fun registerOnBondStatus(): Flow<Int> {
        return bluetoothConnectionRepository.registerOnBondStatus()
    }

    override fun getDeviceEUI(macAddress: String): Either<Failure, String> {
        return bluetoothScannerRepository.getScannedDevice(macAddress)?.devEUI?.let {
            Either.Right(it)
        } ?: Either.Left(DataError.CacheMissError)
    }

    override suspend fun fetchClaimingKey(): Either<Failure, String> {
        return bluetoothConnectionRepository.fetchClaimingKey()
    }
}
