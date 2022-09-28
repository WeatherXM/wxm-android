package com.weatherxm.data.repository.bluetooth

import arrow.core.Either
import com.juul.kable.Peripheral
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.bluetooth.BluetoothConnectionDataSource
import kotlinx.coroutines.flow.Flow

class BluetoothConnectionRepositoryImpl(
    private val dataSource: BluetoothConnectionDataSource
) : BluetoothConnectionRepository {

    override fun setPeripheral(address: String): Either<Failure, Unit> {
        return dataSource.setPeripheral(address)
    }

    override suspend fun connectToPeripheral(): Either<Failure, Peripheral> {
        return dataSource.connectToPeripheral()
    }

    override fun registerOnBondStatus(): Flow<Int> {
        return dataSource.registerOnBondStatus()
    }
}
