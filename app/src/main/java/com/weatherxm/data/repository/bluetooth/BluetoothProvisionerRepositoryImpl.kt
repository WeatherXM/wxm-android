package com.weatherxm.data.repository.bluetooth

import arrow.core.Either
import com.espressif.provisioning.listeners.WiFiScanListener
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.bluetooth.BluetoothProvisionerDataSource
import kotlinx.coroutines.flow.Flow

class BluetoothProvisionerRepositoryImpl(
    private val bluetoothDataSource: BluetoothProvisionerDataSource
) : BluetoothProvisionerRepository {

    override fun scanNetworks(wiFiScanListener: WiFiScanListener) {
        bluetoothDataSource.scanNetworks(wiFiScanListener)
    }

    override fun provision(ssid: String, passphrase: String) {
        bluetoothDataSource.provision(ssid, passphrase)
    }

    override fun registerOnProvisionCompletionStatus(): Flow<Either<Failure, Unit>> {
        return bluetoothDataSource.registerOnProvisionCompletionStatus()
    }
}
