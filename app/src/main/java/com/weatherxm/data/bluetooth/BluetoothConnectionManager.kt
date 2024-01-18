package com.weatherxm.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentFilter.SYSTEM_HIGH_PRIORITY
import android.os.Build
import arrow.core.Either
import com.juul.kable.BluetoothDisabledException
import com.juul.kable.Characteristic
import com.juul.kable.ConnectionLostException
import com.juul.kable.ConnectionRejectedException
import com.juul.kable.Descriptor
import com.juul.kable.GattRequestRejectedException
import com.juul.kable.GattStatusException
import com.juul.kable.NotReadyException
import com.juul.kable.Peripheral
import com.juul.kable.peripheral
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.parcelable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class BluetoothConnectionManager(
    private val context: Context, private val bluetoothAdapter: BluetoothAdapter?
) {
    companion object {
        const val READ_CHARACTERISTIC_UUID = "49616"
        const val WRITE_CHARACTERISTIC_UUID = "34729"
        const val AT_CLAIMING_KEY_COMMAND = "AT+CLAIM_KEY=?\r\n"
        const val AT_DEV_EUI_COMMAND = "AT+DEUI=?\r\n"
        const val AT_SET_FREQUENCY_COMMAND = "AT+BAND="
        const val AT_SET_INTERVAL_COMMAND = "AT+INTERVAL=3\r\n"
        const val AT_REBOOT_COMMAND = "ATZ\r\n"
        val DELAY_BEFORE_CHARACTERISTICS_SETUP = TimeUnit.SECONDS.toMillis(1L)
    }

    private lateinit var peripheral: Peripheral
    private var macAddress: String = ""
    private var readCharacteristic: Characteristic? = null
    private var writeCharacteristic: Characteristic? = null

    private val bondStateChangedFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)

    private val bondStatus = MutableSharedFlow<Int>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST
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
    @OptIn(DelicateCoroutinesApi::class)
    private val bondStateChangedReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val bluetoothDevice =
                    intent.parcelable<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                when (bluetoothDevice?.bondState) {
                    BluetoothDevice.BOND_BONDED -> {
                        Timber.d("[BLE Communication]: Bonded.")
                        GlobalScope.launch {
                            setReadWriteCharacteristic()
                        }
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

    /**
     * Suppress MissingPermission as we will call this function only after we have it granted
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.filter {
            it.name != null && it.name.contains("WeatherXM")
        } ?: mutableListOf()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun setPeripheral(address: String): Either<Failure, Unit> {
        return try {
            macAddress = address
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
            Timber.d("[BLE Communication]: Disconnecting from peripheral...")
            readCharacteristic = null
            writeCharacteristic = null
            peripheral.disconnect()
        } catch (e: UninitializedPropertyAccessException) {
            Timber.d(e, "Could not disconnect peripheral.")
        }
    }

    /**
     * Suppress MissingPermission as we will call this function only after we have it granted
     */
    @SuppressLint("MissingPermission")
    suspend fun connectToPeripheral(): Either<Failure, Unit> {
        return try {
            /*
             * Register the receivers BEFORE trying to connect
             */
            Timber.d("[BLE Communication]: Connecting to peripheral...")
            bondStateChangedFilter.priority = SYSTEM_HIGH_PRIORITY
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    bondStateChangedReceiver, bondStateChangedFilter,
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(bondStateChangedReceiver, bondStateChangedFilter)
            }
            peripheral.connect()

            if (getPairedDevices().any { it.address == macAddress }) {
                withContext(coroutineContext) {
                    delay(DELAY_BEFORE_CHARACTERISTICS_SETUP)
                    setReadWriteCharacteristic()
                }
            }
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
        } catch (e: GattRequestRejectedException) {
            Timber.w(e, "Connection to peripheral failed with GattRequestRejectedException")
            Either.Left(BluetoothError.GattRequestRejectedException())
        } catch (e: GattStatusException) {
            Timber.w(e, "[enable descriptor notification] failed: GattStatusException")
            Either.Left(BluetoothError.GattRequestRejectedException())
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
                    enableDescriptorNotification(it.descriptors[0])
                    readCharacteristic = it
                }
            }
        }

        /**
         * When we get here it means that we are at the equivalent of BLE `onConnected`
         * so we need to run this to set the interval of the weather station.
         */
        setInterval()
    }

    private suspend fun enableDescriptorNotification(descriptor: Descriptor) {
        try {
            peripheral.write(descriptor, ENABLE_NOTIFICATION_VALUE)
        } catch (e: GattRequestRejectedException) {
            Timber.w(e, "[enable descriptor notification] failed: GattRequestRejectedException")
        } catch (e: ConnectionLostException) {
            Timber.w(e, "[enable descriptor notification] failed: ConnectionLostException")
        } catch (e: NotReadyException) {
            Timber.w(e, "[enable descriptor notification] failed: NotReadyException")
        } catch (e: GattStatusException) {
            Timber.w(e, "[enable descriptor notification] failed: GattStatusException")
        }
    }

    private suspend fun setInterval() {
        setATCommand(AT_SET_INTERVAL_COMMAND) {
            it.onRight {
                Timber.d("[BLE Communication] Set Interval success")
            }.onLeft { failure ->
                Timber.e("[BLE Communication] ERROR: At set interval $failure")
            }
        }
    }

    suspend fun write(command: String): Boolean {
        Timber.d("[BLE Communication]: $command")
        return writeCharacteristic?.let {
            try {
                peripheral.write(it, command.toByteArray())
                true
            } catch (e: GattRequestRejectedException) {
                Timber.w(e, "[$command] failed: GattRequestRejectedException")
                false
            } catch (e: ConnectionLostException) {
                Timber.w(e, "[$command] failed: ConnectionLostException")
                false
            } catch (e: GattStatusException) {
                Timber.w(e, "[$command] failed: GattStatusException")
                false
            } catch (e: NotReadyException) {
                Timber.w(e, "[$command] failed: NotReadyException")
                false
            }
        } ?: false
    }

    suspend fun fetchATCommand(command: String, listener: (Either<Failure, String>) -> Unit) {
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
                    listener.invoke(Either.Left(BluetoothError.ATCommandError()))
                } else {
                    listener.invoke(Either.Right(fullResponse))
                }
            }.collect {
                fullResponse += String(it).replace("'", "")
            }
        }
    }

    suspend fun setATCommand(command: String, listener: (Either<Failure, Unit>) -> Unit) {
        if (!write(command)) {
            listener.invoke(Either.Left(BluetoothError.ConnectionRejectedError()))
            return
        }

        readCharacteristic?.let { characteristic ->
            var isSuccess = false
            peripheral.observe(characteristic).cancellable().takeWhile {
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
                    listener.invoke(Either.Left(BluetoothError.ATCommandError()))
                }
            }.catch {
                Timber.e(it)
                listener.invoke(Either.Left(BluetoothError.ATCommandError()))
            }.collect()
        }
    }

    suspend fun reboot(command: String, listener: (Either<Failure, Unit>) -> Unit) {
        if (!write(command)) {
            listener.invoke(Either.Left(BluetoothError.ConnectionRejectedError()))
            return
        }

        readCharacteristic?.let { characteristic ->
            coroutineScope {
                // TODO: Reboot command doesn't seem to do something.
                val job = launch {
                    peripheral.observe(characteristic).cancellable().collect()
                }
                withContext(coroutineContext) {
                    if (command == AT_REBOOT_COMMAND) {
                        job.cancel()
                        listener.invoke(Either.Right(Unit))
                    }
                }
            }
        }
    }
}
