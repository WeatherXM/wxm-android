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
import com.juul.kable.Characteristic
import com.juul.kable.Descriptor
import com.juul.kable.GattRequestRejectedException
import com.juul.kable.GattStatusException
import com.juul.kable.GattWriteException
import com.juul.kable.NotConnectedException
import com.juul.kable.Peripheral
import com.juul.kable.UnmetRequirementException
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.data.models.Failure
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.util.AndroidBuildInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
import kotlin.uuid.ExperimentalUuidApi

@Suppress("TooManyFunctions")
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

    private lateinit var bondStateChangedReceiver: BroadcastReceiver
    private lateinit var peripheral: Peripheral
    private var macAddress: String = String.empty()
    private var readCharacteristic: Characteristic? = null
    private var writeCharacteristic: Characteristic? = null
    private var isSettingCharacteristics: Boolean = false

    private val bondStateChangedFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)

    private val bondStatus = MutableSharedFlow<Int>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun onBondStatus(): Flow<Int> {
        bondStatus.resetReplayCache()
        return bondStatus
    }

    /**
     * Suppress MissingPermission as we will call this function only after we have it granted
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.filter {
            it?.name != null && it.name.contains("WeatherXM")
        } ?: mutableListOf()
    }

    /**
     * This broadcast receiver is a necessity in order to know when our device is BONDED and we can
     * start communicating with it and working on its data
     */
    private fun initBondStateChangeReceiver() {
        bondStateChangedReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                    val bluetoothDevice =
                        intent.parcelable<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    when (bluetoothDevice?.bondState) {
                        BluetoothDevice.BOND_BONDED -> {
                            Timber.d("[BLE Communication]: Bonded.")
                            tryToSetCharacteristics()
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
    }

    fun setPeripheral(address: String): Either<Failure, Unit> {
        return try {
            if (this::peripheral.isInitialized) {
                peripheral.scope.cancel()
            }
            macAddress = address
            initBondStateChangeReceiver()
            peripheral = Peripheral(address)
            Either.Right(Unit)
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Creation of peripheral failed: $address")
            Either.Left(BluetoothError.PeripheralCreationError())
        }
    }

    private fun tryToSetCharacteristics() {
        peripheral.scope.launch {
            isSettingCharacteristics = true
            delay(DELAY_BEFORE_CHARACTERISTICS_SETUP)
            setReadWriteCharacteristic()
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
        } catch (e: NotConnectedException) {
            Timber.d(e, "Could not disconnect peripheral.")
        } catch (e: CancellationException) {
            Timber.d(e, "Could not disconnect peripheral.")
        } catch (e: IllegalStateException) {
            Timber.d(e, "Could not disconnect peripheral.")
        }
    }

    /**
     * Suppress MissingPermission as we will call this function only after we have it granted
     */
    @SuppressLint("InlinedApi")
    suspend fun connectToPeripheral(): Either<Failure, Unit> {
        return try {
            /*
             * Register the receivers BEFORE trying to connect
             */
            Timber.d("[BLE Communication]: Connecting to peripheral...")
            bondStateChangedFilter.priority = SYSTEM_HIGH_PRIORITY
            if (AndroidBuildInfo.sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    bondStateChangedReceiver, bondStateChangedFilter, Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(bondStateChangedReceiver, bondStateChangedFilter)
            }
            peripheral.connect()

            if (getPairedDevices().any { it.address == macAddress }) {
                tryToSetCharacteristics()
            }
            Either.Right(Unit)
        } catch (e: IllegalStateException) {
            Timber.e(e, "Connection to peripheral failed with IllegalStateException")
            Either.Left(BluetoothError.IllegalStateError())
        } catch (e: CancellationException) {
            /**
             * Timber.d because that we do not need to log in our Crashlytics that the device
             * has been disconnected because the user cancelled the connection
             */
            Timber.d(e, "Connection to peripheral failed with CancellationException")
            Either.Left(BluetoothError.CancellationError())
        } catch (e: UnmetRequirementException) {
            Timber.e(e, "Connection to peripheral failed with BluetoothDisabledException")
            Either.Left(BluetoothError.BluetoothDisabledException())
        } catch (e: NotConnectedException) {
            /**
             * Timber.d because that we do not need to log in our Crashlytics that the device
             * has been disconnected probably because it's too far away
             */
            Timber.d(e, "Connection to peripheral failed with ConnectionLostException")
            Either.Left(BluetoothError.ConnectionLostException())
        } catch (e: GattRequestRejectedException) {
            Timber.e(e, "Connection to peripheral failed with GattRequestRejectedException")
            Either.Left(BluetoothError.GattRequestRejectedException())
        } catch (e: GattStatusException) {
            Timber.e(e, "[enable descriptor notification] failed: GattStatusException")
            Either.Left(BluetoothError.GattRequestRejectedException())
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun setReadWriteCharacteristic() {
        if (readCharacteristic != null && writeCharacteristic != null) {
            isSettingCharacteristics = false
            return
        }
        Timber.d("[BLE Communication]: Setting read & write characteristics...")
        peripheral.services.value?.forEach { service ->
            service.characteristics.forEach {
                if (it.characteristicUuid.toString().contains(WRITE_CHARACTERISTIC_UUID)) {
                    writeCharacteristic = it
                } else if (it.characteristicUuid.toString().contains(READ_CHARACTERISTIC_UUID)) {
                    enableDescriptorNotification(it.descriptors[0])
                    readCharacteristic = it
                }
            }
        }
        isSettingCharacteristics = false

        /**
         * When we get here it means that we are at the equivalent of BLE `onConnected`
         * so we need to run this to set the interval of the weather station.
         */
        setInterval()
    }

    private suspend fun enableDescriptorNotification(descriptor: Descriptor) {
        try {
            peripheral.write(descriptor, ENABLE_NOTIFICATION_VALUE)
        } catch (e: GattWriteException) {
            Timber.w(
                e, "[enable descriptor notification]: GattWriteException - Result: ${e.result}"
            )
        } catch (e: GattRequestRejectedException) {
            Timber.w(e, "[enable descriptor notification]: GattRequestRejectedException")
        } catch (e: NotConnectedException) {
            Timber.w(e, "[enable descriptor notification]: ConnectionLostException")
        } catch (e: GattStatusException) {
            Timber.w(e, "[enable descriptor notification]: GattStatusException")
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

    private suspend fun write(command: String): Boolean {
        if (isSettingCharacteristics) {
            /**
             * If we are still in the process of setting characteristics, delay a bit
             * to ensure they are correctly set
             */
            delay(DELAY_BEFORE_CHARACTERISTICS_SETUP * 2)
        }
        Timber.d("[BLE Communication]: $command")
        return writeCharacteristic?.let {
            try {
                peripheral.write(it, command.toByteArray())
                true
            } catch (e: GattWriteException) {
                Timber.w(
                    e,
                    "[$command]: GattWriteException - Result: ${e.result}, Name: ${e.result.name}"
                )
                false
            } catch (e: GattRequestRejectedException) {
                Timber.w(e, "[$command] failed: GattRequestRejectedException")
                false
            } catch (e: NotConnectedException) {
                Timber.w(e, "[$command] failed: ConnectionLostException")
                false
            } catch (e: GattStatusException) {
                Timber.w(e, "[$command] failed: GattStatusException")
                false
            }
        } ?: false
    }

    suspend fun fetchATCommand(command: String, listener: (Either<Failure, String>) -> Unit) {
        if (!write(command)) {
            listener.invoke(Either.Left(BluetoothError.ATCommandError()))
            return
        }

        readCharacteristic?.let { characteristic ->
            var failed = false
            var fullResponse = String.empty()
            peripheral.observe(characteristic).takeWhile {
                val currentResponse =
                    String(it).replace("\r", String.empty()).replace("\n", String.empty())
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
                fullResponse += String(it).replace("'", String.empty())
            }
        }
    }

    suspend fun setATCommand(command: String, listener: (Either<Failure, Unit>) -> Unit) {
        if (!write(command)) {
            listener.invoke(Either.Left(BluetoothError.ATCommandError()))
            return
        }

        readCharacteristic?.let { characteristic ->
            var isSuccess = false
            peripheral.observe(characteristic).cancellable().takeWhile {
                val currentResponse =
                    String(it).replace("\r", String.empty()).replace("\n", String.empty())
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

    /**
     * Custom fix needed for firmware versions < 2.9.0 where ATZ commands
     * does NOT return an OK before rebooting so we return immediately here
     */
    suspend fun reboot(listener: (Either<Failure, Unit>) -> Unit) {
        if (!write(AT_REBOOT_COMMAND)) {
            listener.invoke(Either.Left(BluetoothError.ATCommandError()))
            return
        }

        readCharacteristic?.let { characteristic ->
            coroutineScope {
                val job = launch {
                    peripheral.observe(characteristic).cancellable().collect()
                }
                withContext(coroutineContext) {
                    job.cancel()
                    listener.invoke(Either.Right(Unit))
                }
            }
        }
    }
}
