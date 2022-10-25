package com.weatherxm.usecases

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.juul.kable.Peripheral
import com.weatherxm.data.Failure
import kotlinx.coroutines.flow.Flow

interface BluetoothConnectionUseCase {
    fun setPeripheral(address: String): Either<Failure, Unit>
    suspend fun connectToPeripheral(): Either<Failure, Unit>
    fun registerOnBondStatus(): Flow<Int>
    fun getDeviceEUI(macAddress: String): Either<Failure, String>
    fun getPairedDevices(): List<BluetoothDevice>?
    suspend fun fetchClaimingKey(): Either<Failure, String>
}
