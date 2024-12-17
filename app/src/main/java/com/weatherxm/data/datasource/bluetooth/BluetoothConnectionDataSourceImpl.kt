package com.weatherxm.data.datasource.bluetooth

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import arrow.core.handleErrorWith
import com.weatherxm.data.bluetooth.BluetoothConnectionManager
import com.weatherxm.data.bluetooth.BluetoothConnectionManager.Companion.AT_CLAIMING_KEY_COMMAND
import com.weatherxm.data.bluetooth.BluetoothConnectionManager.Companion.AT_DEV_EUI_COMMAND
import com.weatherxm.data.bluetooth.BluetoothConnectionManager.Companion.AT_SET_FREQUENCY_COMMAND
import com.weatherxm.data.frequencyToHeliumBleBandValue
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Frequency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.suspendCoroutine

class BluetoothConnectionDataSourceImpl(
    private val connectionManager: BluetoothConnectionManager
) : BluetoothConnectionDataSource {
    companion object {
        const val CLAIM_MAX_RETRIES = 3
        const val CLAIM_RETRY_DELAY_MS = 3000L
    }

    override fun getPairedDevices(): List<BluetoothDevice> {
        return connectionManager.getPairedDevices()
    }

    override suspend fun setPeripheral(address: String): Either<Failure, Unit> {
        return connectionManager.setPeripheral(address)
    }

    override suspend fun connectToPeripheral(numOfRetries: Int): Either<Failure, Unit> {
        return connectionManager.connectToPeripheral().handleErrorWith {
            if (it is BluetoothError.ConnectionLostException && numOfRetries < CLAIM_MAX_RETRIES) {
                Timber.d("Connection lost with BLE. Retrying after 3 seconds...")
                delay(CLAIM_RETRY_DELAY_MS)
                connectToPeripheral(numOfRetries + 1)
            } else {
                Either.Left(it)
            }
        }
    }

    override suspend fun disconnectFromPeripheral() {
        connectionManager.disconnectFromPeripheral()
    }

    override fun registerOnBondStatus(): Flow<Int> {
        return connectionManager.onBondStatus()
    }

    override suspend fun fetchClaimingKey(): Either<Failure, String> {
        val coroutineContext = coroutineContext
        return suspendCoroutine { continuation ->
            CoroutineScope(coroutineContext).launch {
                connectionManager.fetchATCommand(AT_CLAIMING_KEY_COMMAND) {
                    continuation.resumeWith(Result.success(it))
                }
            }
        }
    }

    override suspend fun fetchDeviceEUI(): Either<Failure, String> {
        val coroutineContext = coroutineContext
        return suspendCoroutine { continuation ->
            CoroutineScope(coroutineContext).launch {
                connectionManager.fetchATCommand(AT_DEV_EUI_COMMAND) {
                    continuation.resumeWith(Result.success(it))
                }
            }
        }
    }

    override suspend fun setFrequency(frequency: Frequency): Either<Failure, Unit> {
        val coroutineContext = coroutineContext
        return suspendCancellableCoroutine { continuation ->
            CoroutineScope(coroutineContext).launch {
                /**
                 * /r/n is needed so we need to add them at the end of the command here
                 */
                val command =
                    "$AT_SET_FREQUENCY_COMMAND${frequencyToHeliumBleBandValue(frequency)}\r\n"
                connectionManager.setATCommand(command) {
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.success(it))
                    }
                }
            }
        }
    }

    /**
     * Custom fix needed for firmware versions < 2.9.0
     * by calling reboot function and not setATCommandAndResume
     */
    override suspend fun reboot(): Either<Failure, Unit> {
        val coroutineContext = coroutineContext
        return suspendCancellableCoroutine { continuation ->
            CoroutineScope(coroutineContext).launch {
                connectionManager.reboot {
                    continuation.resumeWith(Result.success(it))
                }
            }
        }
    }
}
