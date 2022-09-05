package com.weatherxm.data.bluetooth

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
import timber.log.Timber


class BluetoothConnectionManager {
    companion object {
        const val DEFAULT_PAIR_PIN = "000000"
    }

    private lateinit var peripheral: Peripheral
    private var readCharacteristic: Characteristic? = null
    private var writeCharacteristic: Characteristic? = null

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
            peripheral.connect()

            // TODO: Remove these from here, they are used for testing purposes
            setReadWriteCharacteristic()
            fetchGeneralInfo()
            fetchMeasurement()
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
