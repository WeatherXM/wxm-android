package com.weatherxm.usecases

import android.bluetooth.BluetoothDevice
import android.net.Uri
import arrow.core.Either
import com.juul.kable.Peripheral
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow

interface BluetoothConnectionUseCase {
    fun setPeripheral(bluetoothDevice: BluetoothDevice)
    suspend fun connectToPeripheral(): Either<Failure, Peripheral>
    fun update(updatePackage: Uri): Flow<Int>
    fun registerOnBondStatus(): Flow<Int>
}

class BluetoothConnectionUseCaseImpl(
    private val bluetoothRepository: BluetoothRepository,
) : BluetoothConnectionUseCase {

    override fun setPeripheral(bluetoothDevice: BluetoothDevice) {
        bluetoothRepository.setPeripheral(bluetoothDevice)
    }

    override suspend fun connectToPeripheral(): Either<Failure, Peripheral> {
        return bluetoothRepository.connectToPeripheral()
    }

    override fun update(updatePackage: Uri): Flow<Int> {
        bluetoothRepository.setUpdater()
        return bluetoothRepository.update(updatePackage)
    }

    override fun registerOnBondStatus(): Flow<Int> {
        return bluetoothRepository.registerOnBondStatus()
    }
}
