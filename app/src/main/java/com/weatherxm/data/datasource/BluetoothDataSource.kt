package com.weatherxm.data.datasource

import android.bluetooth.BluetoothDevice
import android.net.Uri
import arrow.core.Either
import com.juul.kable.Advertisement
import com.juul.kable.Identifier
import com.juul.kable.Peripheral
import com.weatherxm.data.Failure
import kotlinx.coroutines.flow.Flow

interface BluetoothDataSource {
    suspend fun scanBleDevices(): Flow<Advertisement>
    fun setPeripheral(identifier: Identifier): Either<Failure, Unit>
    suspend fun connectToPeripheral(): Either<Failure, Peripheral>
    fun setUpdater()
    fun update(updatePackage: Uri): Flow<Int>
}
