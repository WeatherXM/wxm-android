package com.weatherxm.data.datasource

import android.bluetooth.BluetoothDevice
import android.net.Uri
import arrow.core.Either
import com.espressif.provisioning.listeners.WiFiScanListener
import com.juul.kable.Peripheral
import com.weatherxm.data.Failure
import com.weatherxm.data.bluetooth.BluetoothConnectionManager
import com.weatherxm.data.bluetooth.BluetoothProvisioner
import com.weatherxm.data.bluetooth.BluetoothScanner
import com.weatherxm.data.bluetooth.BluetoothUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class BluetoothDataSourceImpl(
    private val scanner: BluetoothScanner,
    private val connectionManager: BluetoothConnectionManager,
    private val provisioner: BluetoothProvisioner,
    private val updater: BluetoothUpdater,
) : BluetoothDataSource {
    private val currentScannedDevices = mutableMapOf<String, BluetoothDevice>()

    override suspend fun registerOnScanning(): Flow<BluetoothDevice> {
        return scanner.registerOnScanning().onEach {
            currentScannedDevices[it.address] = it
        }
    }

    override fun registerOnBondStatus(): Flow<Int> {
        return connectionManager.onBondStatus()
    }

    override fun registerOnProvisionCompletionStatus(): Flow<Either<Failure, Unit>> {
        return provisioner.getProvisioningCompletionStatus()
    }

    override fun registerOnScanCompletionStatus(): Flow<Either<Failure, Unit>> {
        return scanner.getCompletionStatus()
    }

    override fun registerOnUpdateCompletionStatus(): Flow<Either<Failure, Unit>> {
        return updater.getCompletionStatus()
    }

    override suspend fun startScanning() {
        currentScannedDevices.clear()
        scanner.startScanning()
    }

    override fun setPeripheral(address: String): Either<Failure, Unit> {
        return connectionManager.setPeripheral(address)
    }

    override suspend fun connectToPeripheral(): Either<Failure, Peripheral> {
        return connectionManager.connectToPeripheral()
    }

    override fun setUpdater() {
        updater.setUpdater()
    }

    override fun update(updatePackage: Uri): Flow<Int> {
        return updater.update(updatePackage)
    }

    override fun scanNetworks(wiFiScanListener: WiFiScanListener) {
        provisioner.scanNetworks(wiFiScanListener)
    }

    override fun provision(ssid: String, passphrase: String) {
        provisioner.provisionDevice(ssid, passphrase)
    }
}
