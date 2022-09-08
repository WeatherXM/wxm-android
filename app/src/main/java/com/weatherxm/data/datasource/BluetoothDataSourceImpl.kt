package com.weatherxm.data.datasource

import android.bluetooth.BluetoothDevice
import android.net.Uri
import arrow.core.Either
import com.juul.kable.Identifier
import com.juul.kable.Peripheral
import com.weatherxm.data.Failure
import com.weatherxm.data.bluetooth.BluetoothConnectionManager
import com.weatherxm.data.bluetooth.BluetoothScanner
import com.weatherxm.data.bluetooth.BluetoothUpdater
import kotlinx.coroutines.flow.Flow

class BluetoothDataSourceImpl(
    private val scanner: BluetoothScanner,
    private val bluetoothConnectionManager: BluetoothConnectionManager,
    private val bluetoothUpdater: BluetoothUpdater,
) : BluetoothDataSource {

    override suspend fun registerOnScanning(): Flow<BluetoothDevice> {
        return scanner.registerOnScanning()
    }

    override suspend fun startScanning() {
        scanner.startScanning()
    }

    override fun setPeripheral(identifier: Identifier): Either<Failure, Unit> {
        return bluetoothConnectionManager.setPeripheral(identifier)
    }

    override suspend fun connectToPeripheral(): Either<Failure, Peripheral> {
        return bluetoothConnectionManager.connectToPeripheral()
    }

    override fun setUpdater() {
        bluetoothUpdater.setUpdater()
    }

    override fun update(updatePackage: Uri): Flow<Int> {
        return bluetoothUpdater.update(updatePackage)
    }
}
