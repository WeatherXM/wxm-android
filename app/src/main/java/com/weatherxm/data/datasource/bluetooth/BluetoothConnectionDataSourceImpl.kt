package com.weatherxm.data.datasource.bluetooth

import arrow.core.Either
import com.juul.kable.Peripheral
import com.weatherxm.data.Failure
import com.weatherxm.data.bluetooth.BluetoothConnectionManager
import kotlinx.coroutines.flow.Flow

class BluetoothConnectionDataSourceImpl(
    private val connectionManager: BluetoothConnectionManager
) : BluetoothConnectionDataSource {

    override fun setPeripheral(address: String): Either<Failure, Unit> {
        return connectionManager.setPeripheral(address)
    }

    override suspend fun connectToPeripheral(): Either<Failure, Peripheral> {
        return connectionManager.connectToPeripheral()
    }

    override fun registerOnBondStatus(): Flow<Int> {
        return connectionManager.onBondStatus()
    }
}
