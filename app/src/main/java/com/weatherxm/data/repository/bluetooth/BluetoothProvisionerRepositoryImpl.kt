package com.weatherxm.data.repository.bluetooth

import arrow.core.Either
import com.espressif.provisioning.listeners.WiFiScanListener
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.bluetooth.BluetoothProvisionerDataSource

class BluetoothProvisionerRepositoryImpl(
    private val bluetoothDataSource: BluetoothProvisionerDataSource
) : BluetoothProvisionerRepository {

    override fun scanNetworks(wiFiScanListener: WiFiScanListener) {
        bluetoothDataSource.scanNetworks(wiFiScanListener)
    }

    override suspend fun provision(ssid: String, passphrase: String): Either<Failure, Unit> {
        return bluetoothDataSource.provision(ssid, passphrase)
    }
}
