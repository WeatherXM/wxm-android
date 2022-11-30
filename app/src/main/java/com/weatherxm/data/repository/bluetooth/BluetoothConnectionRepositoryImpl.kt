package com.weatherxm.data.repository.bluetooth

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Frequency
import com.weatherxm.data.datasource.bluetooth.BluetoothConnectionDataSource
import kotlinx.coroutines.flow.Flow

class BluetoothConnectionRepositoryImpl(
    private val dataSource: BluetoothConnectionDataSource
) : BluetoothConnectionRepository {

    override fun getPairedDevices(): List<BluetoothDevice> {
        return dataSource.getPairedDevices()
    }

    override fun setPeripheral(address: String): Either<Failure, Unit> {
        return dataSource.setPeripheral(address)
    }

    override suspend fun connectToPeripheral(): Either<Failure, Unit> {
        return dataSource.connectToPeripheral()
    }

    override fun registerOnBondStatus(): Flow<Int> {
        return dataSource.registerOnBondStatus()
    }

    override suspend fun fetchClaimingKey(): Either<Failure, String> {
        return dataSource.fetchClaimingKey()
    }

    override suspend fun fetchDeviceEUI(): Either<Failure, String> {
        return dataSource.fetchDeviceEUI()
    }

    override suspend fun setFrequency(frequency: Frequency): Either<Failure, Unit> {
        return dataSource.setFrequency(frequency)
    }
}
