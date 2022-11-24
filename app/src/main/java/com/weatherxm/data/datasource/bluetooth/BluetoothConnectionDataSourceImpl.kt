package com.weatherxm.data.datasource.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Frequency
import com.weatherxm.data.bluetooth.BluetoothConnectionManager
import com.weatherxm.data.bluetooth.BluetoothConnectionManager.Companion.AT_CLAIMING_KEY_COMMAND
import com.weatherxm.data.bluetooth.BluetoothConnectionManager.Companion.AT_DEV_EUI_COMMAND
import com.weatherxm.data.bluetooth.BluetoothConnectionManager.Companion.AT_SET_FREQUENCY_COMMAND
import com.weatherxm.data.frequencyToHeliumBleBandValue
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.suspendCoroutine

class BluetoothConnectionDataSourceImpl(
    private val connectionManager: BluetoothConnectionManager,
    private val bluetoothAdapter: BluetoothAdapter?
) : BluetoothConnectionDataSource {

    /**
     * Suppress MissingPermission as we will call this function only after we have it granted
     */
    @SuppressLint("MissingPermission")
    override fun getPairedDevices(): List<BluetoothDevice>? {
        return bluetoothAdapter?.bondedDevices?.filter {
            it.name.contains("WeatherXM")
        }
    }

    override fun setPeripheral(address: String): Either<Failure, Unit> {
        return connectionManager.setPeripheral(address)
    }

    override suspend fun connectToPeripheral(): Either<Failure, Unit> {
        return connectionManager.connectToPeripheral()
    }

    override fun registerOnBondStatus(): Flow<Int> {
        return connectionManager.onBondStatus()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun fetchClaimingKey(): Either<Failure, String> {
        return suspendCoroutine { continuation ->
            GlobalScope.launch {
                connectionManager.fetchATCommand(AT_CLAIMING_KEY_COMMAND) {
                    continuation.resumeWith(Result.success(it))
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun fetchDeviceEUI(): Either<Failure, String> {
        return suspendCoroutine { continuation ->
            GlobalScope.launch {
                connectionManager.fetchATCommand(AT_DEV_EUI_COMMAND) {
                    continuation.resumeWith(Result.success(it))
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun setFrequency(frequency: Frequency): Either<Failure, Unit> {
        return suspendCoroutine { continuation ->
            GlobalScope.launch {
                /**
                 * /r/n is needed so we need to add them at the end of the command here
                 */
                val command =
                    "$AT_SET_FREQUENCY_COMMAND${frequencyToHeliumBleBandValue(frequency)}\r\n"
                connectionManager.setATCommand(command) {
                    continuation.resumeWith(Result.success(it))
                }
            }
        }
    }
}
