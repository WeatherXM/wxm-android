package com.weatherxm.data.repository.bluetooth

import arrow.core.Either
import com.espressif.provisioning.listeners.WiFiScanListener
import com.weatherxm.data.Failure

interface BluetoothProvisionerRepository {
    fun scanNetworks(wiFiScanListener: WiFiScanListener)
    suspend fun provision(ssid: String, passphrase: String): Either<Failure, Unit>
}
