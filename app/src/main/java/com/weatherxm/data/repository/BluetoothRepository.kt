package com.weatherxm.data.repository

import android.bluetooth.BluetoothDevice
import android.net.Uri
import arrow.core.Either
import com.espressif.provisioning.listeners.WiFiScanListener
import com.juul.kable.Peripheral
import com.weatherxm.data.Failure
import kotlinx.coroutines.flow.Flow

interface BluetoothRepository {
    suspend fun registerOnScanning(): Flow<BluetoothDevice>
    suspend fun startScanning()
    fun setPeripheral(bluetoothDevice: BluetoothDevice): Either<Failure, Unit>
    suspend fun connectToPeripheral(): Either<Failure, Peripheral>
    fun registerOnBondStatus(): Flow<Int>
    fun setUpdater()
    fun update(updatePackage: Uri): Flow<Int>
    fun scanNetworks(wiFiScanListener: WiFiScanListener)
    fun provision(ssid: String, passphrase: String)
    fun registerOnProvisionCompletionStatus(): Flow<Either<Failure, Unit>>
    fun registerOnScanCompletionStatus(): Flow<Either<Failure, Unit>>
    fun registerOnUpdateCompletionStatus(): Flow<Either<Failure, Unit>>
}
