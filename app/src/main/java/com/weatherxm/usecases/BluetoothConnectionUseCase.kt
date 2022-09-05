package com.weatherxm.usecases

import android.bluetooth.BluetoothDevice
import android.net.Uri
import arrow.core.Either
import com.juul.kable.Identifier
import com.juul.kable.Peripheral
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow

interface BluetoothConnectionUseCase {
    fun setPeripheral(identifier: Identifier)
    suspend fun connectToPeripheral(): Either<Failure, Peripheral>
    fun update(updatePackage: Uri): Flow<Int>
}

class BluetoothConnectionUseCaseImpl(
    private val bluetoothRepository: BluetoothRepository,
) : BluetoothConnectionUseCase {

    override fun setPeripheral(identifier: Identifier) {
        bluetoothRepository.setPeripheral(identifier)
    }

    override suspend fun connectToPeripheral(): Either<Failure, Peripheral> {
        return bluetoothRepository.connectToPeripheral()
    }

    override fun update(updatePackage: Uri): Flow<Int> {
        bluetoothRepository.setUpdater()
        return bluetoothRepository.update(updatePackage)
    }
}
