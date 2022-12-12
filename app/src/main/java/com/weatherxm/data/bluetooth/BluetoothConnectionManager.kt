package com.weatherxm.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentFilter.SYSTEM_HIGH_PRIORITY
import arrow.core.Either
import com.juul.kable.BluetoothDisabledException
import com.juul.kable.Characteristic
import com.juul.kable.ConnectionLostException
import com.juul.kable.ConnectionRejectedException
import com.juul.kable.GattRequestRejectedException
import com.juul.kable.Peripheral
import com.juul.kable.peripheral
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import timber.log.Timber

class BluetoothConnectionManager(private val context: Context) {
    companion object {
        const val READ_CHARACTERISTIC_UUID = "49616"
        const val WRITE_CHARACTERISTIC_UUID = "34729"
        const val AT_CLAIMING_KEY_COMMAND = "AT+CLAIM_KEY=?\r\n"
        const val AT_DEV_EUI_COMMAND = "AT+DEUI=?\r\n"
        const val AT_SET_FREQUENCY_COMMAND = "AT+BAND="
    }

    private lateinit var peripheral: Peripheral
    private var readCharacteristic: Characteristic? = null
    private var writeCharacteristic: Characteristic? = null

    private val bondStateChangedFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)

    private val bondStatus = MutableSharedFlow<Int>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_LATEST
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun onBondStatus(): Flow<Int> {
        bondStatus.resetReplayCache()
        return bondStatus
    }

    /*
    * This broadcast receiver is a necessity in order to know when our device is BONDED and we can
    * start communicating with it and working on its data
     */
    private val bondStateChangedReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val bluetoothDevice =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                when (bluetoothDevice?.bondState) {
                    BluetoothDevice.BOND_BONDED -> {
                        Timber.d("[BLE Communication]: Bonded.")
                        bondStatus.tryEmit(BluetoothDevice.BOND_BONDED)
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        Timber.d("[BLE Communication]: Bonding...")
                        bondStatus.tryEmit(BluetoothDevice.BOND_BONDING)
                    }
                    BluetoothDevice.BOND_NONE -> {
                        Timber.d("[BLE Communication]: Bonding NONE...")
                        bondStatus.tryEmit(BluetoothDevice.BOND_NONE)
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun setPeripheral(address: String): Either<Failure, Unit> {
        return try {
            peripheral = GlobalScope.peripheral(address)
            Either.Right(Unit)
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Creation of peripheral failed: $address")
            Either.Left(BluetoothError.PeripheralCreationError())
        }
    }

    fun getPeripheral(): Peripheral {
        return peripheral
    }

    suspend fun disconnectFromPeripheral() {
        try {
            peripheral.disconnect()
        } catch (e: UninitializedPropertyAccessException) {
            Timber.d(e, "Could not disconnect peripheral.")
        }
    }

    suspend fun connectToPeripheral(): Either<Failure, Unit> {
        return try {
            /*
             * Register the receivers BEFORE trying to connect
             */
            bondStateChangedFilter.priority = SYSTEM_HIGH_PRIORITY
            context.registerReceiver(bondStateChangedReceiver, bondStateChangedFilter)
            peripheral.connect()
            Either.Right(Unit)
        } catch (e: ConnectionRejectedException) {
            Timber.w(e, "Connection to peripheral failed with ConnectionRejectedException")
            Either.Left(BluetoothError.ConnectionRejectedError())
        } catch (e: CancellationException) {
            Timber.w(e, "Connection to peripheral failed with CancellationException")
            Either.Left(BluetoothError.CancellationError())
        } catch (e: BluetoothDisabledException) {
            Timber.w(e, "Connection to peripheral failed with BluetoothDisabledException")
            Either.Left(BluetoothError.BluetoothDisabledException())
        } catch (e: ConnectionLostException) {
            Timber.w(e, "Connection to peripheral failed with ConnectionLostException")
            Either.Left(BluetoothError.ConnectionLostException())
        }
    }

    private suspend fun setReadWriteCharacteristic() {
        if (readCharacteristic != null && writeCharacteristic != null) {
            return
        }
        Timber.d("[BLE Communication]: Setting read & write characteristics...")
        peripheral.services?.forEach { service ->
            service.characteristics.forEach {
                if (it.characteristicUuid.toString().contains(WRITE_CHARACTERISTIC_UUID)) {
                    writeCharacteristic = it
                } else if (it.characteristicUuid.toString().contains(READ_CHARACTERISTIC_UUID)) {
                    peripheral.write(it.descriptors[0], ENABLE_NOTIFICATION_VALUE)
                    readCharacteristic = it
                }
            }
        }
    }

    private suspend fun write(command: String): Boolean {
        return writeCharacteristic?.let {
            try {
                peripheral.write(it, command.toByteArray())
                true
            } catch (e: GattRequestRejectedException) {
                Timber.w(e, "[$command] failed: GattRequestRejectedException")
                false
            }
        } ?: false
    }

    suspend fun fetchATCommand(command: String, listener: (Either<Failure, String>) -> Unit) {
        setReadWriteCharacteristic()

        Timber.d("[BLE Communication]: $command")

        if (!write(command)) {
            listener.invoke(Either.Left(BluetoothError.ConnectionRejectedError()))
            return
        }

        readCharacteristic?.let { characteristic ->
            var failed = false
            var fullResponse = ""
            peripheral.observe(characteristic).takeWhile {
                val currentResponse = String(it).replace("\r", "").replace("\n", "")
                Timber.d("[BLE Communication] Response: $currentResponse")
                if (currentResponse.contains("ERROR")) {
                    Timber.e("[BLE Communication] ERROR: $currentResponse")
                    failed = true
                    return@takeWhile false
                }
                currentResponse != "OK"
            }.onCompletion {
                Timber.d("[BLE Communication] Full Response: $fullResponse")
                if (failed) {
                    listener.invoke(Either.Left(BluetoothError.ATCommandError))
                } else {
                    listener.invoke(Either.Right(fullResponse))
                }
            }.collect {
                fullResponse += String(it).replace("'", "")
            }
        }
    }

    suspend fun setATCommand(command: String, listener: (Either<Failure, Unit>) -> Unit) {
        setReadWriteCharacteristic()

        Timber.d("[BLE Communication]: $command")

        if (!write(command)) {
            listener.invoke(Either.Left(BluetoothError.ConnectionRejectedError()))
            return
        }

        readCharacteristic?.let { characteristic ->
            var isSuccess = false
            peripheral.observe(characteristic).takeWhile {
                val currentResponse = String(it).replace("\r", "").replace("\n", "")
                if (currentResponse.contains("ERROR")) {
                    Timber.e("[BLE Communication] ERROR: $currentResponse")
                    isSuccess = false
                    return@takeWhile false
                } else if (currentResponse == "OK") {
                    isSuccess = true
                    return@takeWhile false
                }
                true
            }.onCompletion {
                Timber.d("[BLE Communication]: Success: $isSuccess")
                if (isSuccess) {
                    listener.invoke(Either.Right(Unit))
                } else {
                    listener.invoke(Either.Left(BluetoothError.ATCommandError))
                }
            }.collect()
        }
    }
}
