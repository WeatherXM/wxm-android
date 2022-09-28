package com.weatherxm.usecases

import arrow.core.Either
import com.juul.kable.Peripheral
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.bluetooth.BluetoothConnectionRepository
import kotlinx.coroutines.flow.Flow

interface BluetoothConnectionUseCase {
    fun setPeripheral(address: String): Either<Failure, Unit>
    suspend fun connectToPeripheral(): Either<Failure, Peripheral>
    fun registerOnBondStatus(): Flow<Int>
}

class BluetoothConnectionUseCaseImpl(
    private val bluetoothConnectionRepository: BluetoothConnectionRepository,
) : BluetoothConnectionUseCase {

    override fun setPeripheral(address: String): Either<Failure, Unit> {
        return bluetoothConnectionRepository.setPeripheral(address)
    }

    override suspend fun connectToPeripheral(): Either<Failure, Peripheral> {
        return bluetoothConnectionRepository.connectToPeripheral()
    }

    override fun registerOnBondStatus(): Flow<Int> {
        return bluetoothConnectionRepository.registerOnBondStatus()
    }
}
