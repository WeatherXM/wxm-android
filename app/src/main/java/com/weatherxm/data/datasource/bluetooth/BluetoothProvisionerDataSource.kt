package com.weatherxm.data.datasource.bluetooth

import arrow.core.Either
import com.espressif.provisioning.listeners.WiFiScanListener
import com.weatherxm.data.Failure
import kotlinx.coroutines.flow.Flow

interface BluetoothProvisionerDataSource {
    fun scanNetworks(wiFiScanListener: WiFiScanListener)
    fun provision(ssid: String, passphrase: String)
    fun registerOnProvisionCompletionStatus(): Flow<Either<Failure, Unit>>
}
