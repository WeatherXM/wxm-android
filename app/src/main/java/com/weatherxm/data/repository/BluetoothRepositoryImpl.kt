package com.weatherxm.data.repository

import android.bluetooth.BluetoothDevice
import android.net.Uri
import arrow.core.Either
import com.espressif.provisioning.listeners.WiFiScanListener
import com.juul.kable.Peripheral
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.BluetoothDataSource
import kotlinx.coroutines.flow.Flow

class BluetoothRepositoryImpl(
    private val bluetoothDataSource: BluetoothDataSource
) : BluetoothRepository {

    /*
    * Suppress this because we have asked for permissions already before we reach here.
    */
    @Suppress("MissingPermission")
    override suspend fun registerOnScanning(): Flow<BluetoothDevice> {
        return bluetoothDataSource.registerOnScanning()
    }

    override suspend fun startScanning() {
        bluetoothDataSource.startScanning()
    }

    override fun setPeripheral(address: String): Either<Failure, Unit> {
        return bluetoothDataSource.setPeripheral(address)
    }

    override suspend fun connectToPeripheral(): Either<Failure, Peripheral> {
        return bluetoothDataSource.connectToPeripheral()
    }

    override fun setUpdater() {
        bluetoothDataSource.setUpdater()
    }

    override fun update(updatePackage: Uri): Flow<Int> {
        return bluetoothDataSource.update(updatePackage)
    }

    override fun scanNetworks(wiFiScanListener: WiFiScanListener) {
        bluetoothDataSource.scanNetworks(wiFiScanListener)
    }

    override fun provision(ssid: String, passphrase: String) {
        bluetoothDataSource.provision(ssid, passphrase)
    }

    override fun registerOnProvisionCompletionStatus(): Flow<Either<Failure, Unit>> {
        return bluetoothDataSource.registerOnProvisionCompletionStatus()
    }

    override fun registerOnScanCompletionStatus(): Flow<Either<Failure, Unit>> {
        return bluetoothDataSource.registerOnScanCompletionStatus()
    }

    override fun registerOnUpdateCompletionStatus(): Flow<Either<Failure, Unit>> {
        return bluetoothDataSource.registerOnUpdateCompletionStatus()
    }

    override fun registerOnBondStatus(): Flow<Int> {
        return bluetoothDataSource.registerOnBondStatus()
    }
}
