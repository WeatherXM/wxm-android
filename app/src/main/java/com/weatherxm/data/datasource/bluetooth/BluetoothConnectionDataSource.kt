package com.weatherxm.data.datasource.bluetooth

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.juul.kable.Advertisement
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Frequency
import kotlinx.coroutines.flow.Flow

interface BluetoothConnectionDataSource {
    suspend fun setPeripheral(
        advertisement: Advertisement,
        address: String
    ): Either<Failure, Unit>

    suspend fun connectToPeripheral(numOfRetries: Int = 0): Either<Failure, Unit>
    suspend fun disconnectFromPeripheral()
    fun registerOnBondStatus(): Flow<Int>
    fun getPairedDevices(): List<BluetoothDevice>
    suspend fun fetchClaimingKey(): Either<Failure, String>
    suspend fun fetchDeviceEUI(): Either<Failure, String>
    suspend fun setFrequency(frequency: Frequency): Either<Failure, Unit>
    suspend fun reboot(): Either<Failure, Unit>
}
