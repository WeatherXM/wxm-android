package com.weatherxm.data.datasource.bluetooth

import arrow.core.Either
import com.espressif.provisioning.listeners.WiFiScanListener
import com.weatherxm.data.Failure
import com.weatherxm.data.bluetooth.BluetoothProvisioner

class BluetoothProvisionerDataSourceImpl(
    private val provisioner: BluetoothProvisioner
) : BluetoothProvisionerDataSource {

    override fun scanNetworks(wiFiScanListener: WiFiScanListener) {
        provisioner.scanNetworks(wiFiScanListener)
    }

    override suspend fun provision(ssid: String, passphrase: String): Either<Failure, Unit> {
        return provisioner.provisionDevice(ssid, passphrase)
    }
}
