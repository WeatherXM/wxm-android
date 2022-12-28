package com.weatherxm.data.repository.bluetooth

import arrow.core.Either
import com.espressif.provisioning.WiFiAccessPoint
import com.weatherxm.data.Failure

interface BluetoothProvisionerRepository {
    suspend fun scanNetworks(): Either<Failure, ArrayList<WiFiAccessPoint>?>
    suspend fun provision(ssid: String, passphrase: String): Either<Failure, Unit>
}
