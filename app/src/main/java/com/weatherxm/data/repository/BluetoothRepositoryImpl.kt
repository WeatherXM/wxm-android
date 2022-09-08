package com.weatherxm.data.repository

import android.bluetooth.BluetoothDevice
import android.net.Uri
import arrow.core.Either
import com.juul.kable.Identifier
import com.juul.kable.Peripheral
import com.weatherxm.data.Failure
import com.weatherxm.data.bluetooth.BluetoothConnectionManager
import com.weatherxm.data.datasource.BluetoothDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

class BluetoothRepositoryImpl(
    private val bluetoothDataSource: BluetoothDataSource,
    private val bluetoothConnectionManager: BluetoothConnectionManager
) : BluetoothRepository {

    /*
    * Suppress this because we have asked for permissions already before we reach here.
    */
    @Suppress("MissingPermission")
    override suspend fun registerOnScanning(): Flow<BluetoothDevice> {
        // TODO: Replace this naive filtering.
        return bluetoothDataSource.registerOnScanning().filter {
            it.name.contains("WXM")
        }
    }

    override suspend fun startScanning() {
        bluetoothDataSource.startScanning()
    }

    override fun setPeripheral(identifier: Identifier): Either<Failure, Unit> {
        return bluetoothConnectionManager.setPeripheral(identifier)
    }

    override suspend fun connectToPeripheral(): Either<Failure, Peripheral> {
        return bluetoothConnectionManager.connectToPeripheral()
    }

    override fun setUpdater() {
        bluetoothDataSource.setUpdater()
    }

    override fun update(updatePackage: Uri): Flow<Int> {
        return bluetoothDataSource.update(updatePackage)
    }
}
