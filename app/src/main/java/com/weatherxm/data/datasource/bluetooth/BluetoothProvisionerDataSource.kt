package com.weatherxm.data.datasource.bluetooth

import arrow.core.Either
import com.espressif.provisioning.WiFiAccessPoint
import com.weatherxm.data.Failure

interface BluetoothProvisionerDataSource {
    suspend fun scanNetworks(): Either<Failure, ArrayList<WiFiAccessPoint>?>
    suspend fun provision(ssid: String, passphrase: String): Either<Failure, Unit>
}
