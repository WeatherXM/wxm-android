package com.weatherxm.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
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
import kotlinx.coroutines.flow.takeWhile
import timber.log.Timber

class BluetoothConnectionManager(private val context: Context) {
    companion object {
        const val READ_CHARACTERISTIC_UUID = "49616"
        const val WRITE_CHARACTERISTIC_UUID = "34729"
        const val AT_CLAIMING_KEY_COMMAND = "AT+CLAIM_KEY=?\r\n"
        const val AT_DEV_EUI_COMMAND = "ΑΤ+DevEUI_get=?\r\n"
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
                        Timber.d("BLE Bonded.")
                        bondStatus.tryEmit(BluetoothDevice.BOND_BONDED)
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        Timber.d("BLE Bonding...")
                        bondStatus.tryEmit(BluetoothDevice.BOND_BONDING)
                    }
                    BluetoothDevice.BOND_NONE -> {
                        Timber.d("BLE Bonding NONE...")
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
            Either.Left(BluetoothError.PeripheralCreationError)
        }
    }

    fun getPeripheral(): Peripheral {
        return peripheral
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
            Either.Left(BluetoothError.ConnectionRejectedError)
        } catch (e: CancellationException) {
            Timber.w(e, "Connection to peripheral failed with CancellationException")
            Either.Left(BluetoothError.CancellationError)
        } catch (e: BluetoothDisabledException) {
            Timber.w(e, "Connection to peripheral failed with BluetoothDisabledException")
            Either.Left(BluetoothError.BluetoothDisabledException)
        } catch (e: ConnectionLostException) {
            Timber.w(e, "Connection to peripheral failed with ConnectionLostException")
            Either.Left(BluetoothError.ConnectionLostException)
        }
    }

    // TODO: If not needed, remove it.
    suspend fun disconnectPeripheral() {
        peripheral.disconnect()
    }

    private fun setReadWriteCharacteristic() {
        if (readCharacteristic != null && writeCharacteristic != null) {
            return
        }
        Timber.d("[BLE Communication] Setting read & write characteristics...")
        peripheral.services?.forEach { service ->
            service.characteristics.forEach {
                if (it.characteristicUuid.toString().contains(WRITE_CHARACTERISTIC_UUID)) {
                    writeCharacteristic = it
                } else if (it.characteristicUuid.toString().contains(READ_CHARACTERISTIC_UUID)) {
                    readCharacteristic = it
                }
            }
        }
    }

    suspend fun fetchATCommand(command: String, listener: (Either<Failure, String>) -> Unit) {
        setReadWriteCharacteristic()

        Timber.d("========== [BLE Communication]: $command")

        writeCharacteristic?.let {
            try {
                peripheral.write(it, command.toByteArray())
            } catch (e: GattRequestRejectedException) {
                Timber.w(e, "[$command] failed: GattRequestRejectedException")
                listener.invoke(Either.Left(BluetoothError.ConnectionRejectedError))
            }
        }

        val flowResponse = readCharacteristic?.let {
            peripheral.observe(it)
        }

        flowResponse
            ?.takeWhile {
                val currentResponse = String(it).replace("\r", "").replace("\n", "")
                if (currentResponse.contains("ERROR")) {
                    Timber.w("[BLE Communication] ERROR: $currentResponse")
                    listener.invoke(Either.Left(BluetoothError.ATCommandError))
                    return@takeWhile false
                }
                currentResponse != "OK"
            }
            ?.collect {
                Timber.d("========== [BLE Communication] Response: ${String(it)}")
                listener.invoke(Either.Right(String(it)))
            }
    }
}
