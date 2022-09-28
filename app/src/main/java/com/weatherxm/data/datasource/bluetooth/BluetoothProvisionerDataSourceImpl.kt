package com.weatherxm.data.datasource.bluetooth

import arrow.core.Either
import com.espressif.provisioning.listeners.WiFiScanListener
import com.weatherxm.data.Failure
import com.weatherxm.data.bluetooth.BluetoothProvisioner
import kotlinx.coroutines.flow.Flow

class BluetoothProvisionerDataSourceImpl(
    private val provisioner: BluetoothProvisioner
) : BluetoothProvisionerDataSource {

    override fun registerOnProvisionCompletionStatus(): Flow<Either<Failure, Unit>> {
        return provisioner.getProvisioningCompletionStatus()
    }

    override fun scanNetworks(wiFiScanListener: WiFiScanListener) {
        provisioner.scanNetworks(wiFiScanListener)
    }

    override fun provision(ssid: String, passphrase: String) {
        provisioner.provisionDevice(ssid, passphrase)
    }
}
