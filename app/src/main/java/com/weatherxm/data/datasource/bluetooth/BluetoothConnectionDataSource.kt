package com.weatherxm.data.datasource.bluetooth

import arrow.core.Either
import com.juul.kable.Peripheral
import com.weatherxm.data.Failure
import kotlinx.coroutines.flow.Flow

interface BluetoothConnectionDataSource {
    fun setPeripheral(address: String): Either<Failure, Unit>
    suspend fun connectToPeripheral(): Either<Failure, Peripheral>
    fun registerOnBondStatus(): Flow<Int>
}
