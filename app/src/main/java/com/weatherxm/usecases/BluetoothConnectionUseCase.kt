package com.weatherxm.usecases

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Frequency
import kotlinx.coroutines.flow.Flow

interface BluetoothConnectionUseCase {
    suspend fun setPeripheral(address: String): Either<Failure, Unit>
    suspend fun connectToPeripheral(): Either<Failure, Unit>
    suspend fun disconnectFromPeripheral()
    fun registerOnBondStatus(): Flow<Int>
    fun getPairedDevices(): List<BluetoothDevice>
    suspend fun fetchClaimingKey(): Either<Failure, String>
    suspend fun fetchDeviceEUI(): Either<Failure, String>
    suspend fun setFrequency(frequency: Frequency): Either<Failure, Unit>
    suspend fun reboot(): Either<Failure, Unit>
}
