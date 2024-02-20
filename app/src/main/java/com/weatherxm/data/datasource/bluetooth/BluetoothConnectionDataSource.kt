package com.weatherxm.data.datasource.bluetooth

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Frequency
import kotlinx.coroutines.flow.Flow

interface BluetoothConnectionDataSource {
    suspend fun setPeripheral(address: String): Either<Failure, Unit>
    suspend fun connectToPeripheral(numOfRetries: Int = 0): Either<Failure, Unit>
    suspend fun disconnectFromPeripheral()
    fun registerOnBondStatus(): Flow<Int>
    fun getPairedDevices(): List<BluetoothDevice>
    suspend fun fetchClaimingKey(): Either<Failure, String>
    suspend fun fetchDeviceEUI(): Either<Failure, String>
    suspend fun setFrequency(frequency: Frequency): Either<Failure, Unit>
    suspend fun reboot(): Either<Failure, Unit>
}
