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
import com.juul.kable.Identifier
import com.juul.kable.Peripheral
import com.juul.kable.peripheral
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import timber.log.Timber


class BluetoothConnectionManager(private val context: Context) {
    private val defaultBlePin = "000000"

    private lateinit var peripheral: Peripheral
    private var readCharacteristic: Characteristic? = null
    private var writeCharacteristic: Characteristic? = null

    private val pairingRequestFilter = IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST)
    private val bondStateChangedFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)

    /*
    * This broadcast receiver is used to intercept the BLE PIN prompt as we know that already
    * and we want to input it automatically.
     */
    private val pairingRequestBroadcastReceiver = object : BroadcastReceiver() {
        /*
        * Suppress this because we have asked for permissions already before we reach here.
         */
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_PAIRING_REQUEST) {
                val type =
                    intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR)

                if (type == BluetoothDevice.PAIRING_VARIANT_PIN) {
                    Timber.d("Auto entering BLE PIN of the device")
                    val bluetoothDevice =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    bluetoothDevice?.setPin(defaultBlePin.toByteArray())
                    abortBroadcast()
                }
            }
        }
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
                        Timber.d("Bonded.")
                        /*
                        * Any communication or work with the BLE device that needs to be done
                        * should be done after we reach this point where the BLE device is bonded
                         */
                        // TODO: Remove these. They are being used for testing purposes.
                        GlobalScope.launch {
                            setReadWriteCharacteristic()
                            fetchGeneralInfo()
                            fetchMeasurement()
                        }
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        Timber.d("Bonding...")
                    }
                    BluetoothDevice.BOND_NONE -> {
                        Timber.d("Bonding NONE...")
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun setPeripheral(identifier: Identifier): Either<Failure, Unit> {
        return try {
            peripheral = GlobalScope.peripheral(identifier)
            Either.Right(Unit)
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Creation of peripheral failed: $identifier")
            Either.Left(BluetoothError.PeripheralCreationError)
        }
    }

    fun getPeripheral(): Peripheral {
        return peripheral
    }

    suspend fun connectToPeripheral(): Either<Failure, Peripheral> {
        return try {
            /*
             * Register the receivers BEFORE trying to connect
             */
            pairingRequestFilter.priority = SYSTEM_HIGH_PRIORITY
            bondStateChangedFilter.priority = SYSTEM_HIGH_PRIORITY
            context.registerReceiver(pairingRequestBroadcastReceiver, pairingRequestFilter)
            context.registerReceiver(bondStateChangedReceiver, bondStateChangedFilter)
            peripheral.connect()
            Either.Right(peripheral)
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
        peripheral.services?.forEach { service ->
            service.characteristics.forEach {
                if (it.characteristicUuid.toString().contains("34729")) {
                    writeCharacteristic = it
                } else if (it.characteristicUuid.toString().contains("49616")) {
                    readCharacteristic = it
                }
            }
        }
    }

    private suspend fun fetchGeneralInfo() {
        val command = "AT+CONFIG=?\r\n".toByteArray()
        writeCharacteristic?.let {
            // TODO: Handle exception on write
            peripheral.write(it, command)
        }

        val flowResponse = readCharacteristic?.let {
            peripheral.observe(it)
        }

        var fullResponse = ""
        flowResponse
            ?.takeWhile {
                String(it).replace("\r", "").replace("\n", "") != "OK"
            }
            ?.onCompletion {
                Timber.d("General Info Response: $fullResponse")
            }
            ?.collect {
                fullResponse += String(it)
            }
    }

    private suspend fun fetchMeasurement() {
        val command = "AT+MEA=?\r\n".toByteArray()
        writeCharacteristic?.let {
            // TODO: Handle exception on write
            peripheral.write(it, command)
        }

        val flowResponse = readCharacteristic?.let {
            peripheral.observe(it)
        }

        var fullResponse = ""
        flowResponse
            ?.takeWhile {
                String(it).replace("\r", "").replace("\n", "") != "OK"
            }
            ?.onCompletion {
                Timber.d("Measurement Response: $fullResponse")
            }
            ?.collect {
                fullResponse += String(it)
            }
    }
}
