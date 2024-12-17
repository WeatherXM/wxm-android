package com.weatherxm.data.repository.bluetooth

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.weatherxm.data.datasource.bluetooth.BluetoothConnectionDataSource
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Frequency
import kotlinx.coroutines.flow.Flow

class BluetoothConnectionRepositoryImpl(
    private val dataSource: BluetoothConnectionDataSource
) : BluetoothConnectionRepository {

    override fun getPairedDevices(): List<BluetoothDevice> {
        return dataSource.getPairedDevices()
    }

    override suspend fun setPeripheral(address: String): Either<Failure, Unit> {
        return dataSource.setPeripheral(address)
    }

    override suspend fun connectToPeripheral(): Either<Failure, Unit> {
        return dataSource.connectToPeripheral()
    }

    override suspend fun disconnectFromPeripheral() {
        dataSource.disconnectFromPeripheral()
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

    override suspend fun reboot(): Either<Failure, Unit> {
        return dataSource.reboot().onRight {
            /**
             * Custom fix needed for firmware versions < 2.3.0 where ATZ commands
             * does NOT return an OK before rebooting so in Connection Manager we returned
             * immediately Either.Right(Unit) so here we should reboot implicitly by disconnecting
             */
            dataSource.disconnectFromPeripheral()
        }
    }
}
