package com.weatherxm.data.datasource.bluetooth

import arrow.core.Either
import com.espressif.provisioning.listeners.WiFiScanListener
import com.weatherxm.data.Failure

interface BluetoothProvisionerDataSource {
    fun scanNetworks(wiFiScanListener: WiFiScanListener)
    suspend fun provision(ssid: String, passphrase: String): Either<Failure, Unit>
}
