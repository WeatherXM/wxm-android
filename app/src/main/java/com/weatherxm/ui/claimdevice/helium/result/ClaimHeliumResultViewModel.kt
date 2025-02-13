package com.weatherxm.ui.claimdevice.helium.result

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Frequency
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.unmask
import com.weatherxm.ui.components.BluetoothHeliumViewModel
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ClaimHeliumResultViewModel(
    connectionUseCase: BluetoothConnectionUseCase,
    private val resources: Resources,
    analytics: AnalyticsWrapper,
    dispatcher: CoroutineDispatcher
) : BluetoothHeliumViewModel(String.empty(), null, connectionUseCase, analytics, dispatcher) {
    companion object {
        val CONNECT_DELAY_ON_REBOOT = TimeUnit.SECONDS.toMillis(10L)
    }

    private val onBLEError = MutableLiveData<UIError>()
    private val onBLEConnection = MutableLiveData<Boolean>()
    private val onRebooting = MutableLiveData<Boolean>()
    private val onBLEDevEUI = MutableLiveData<String>()
    private val onBLEClaimingKey = MutableLiveData<String>()

    fun onBLEError() = onBLEError
    fun onBLEConnection() = onBLEConnection
    fun onRebooting() = onRebooting
    fun onBLEDevEUI() = onBLEDevEUI
    fun onBLEClaimingKey() = onBLEClaimingKey

    override fun onConnected() {
        fetchDeviceEUI()
    }

    override fun onConnectionFailure(failure: Failure) {
        onBLEError.postValue(when (failure) {
            is BluetoothError.BluetoothDisabledException -> {
                UIError(resources.getString(R.string.helium_bluetooth_disabled)) {
                    connectToPeripheral()
                }
            }
            is BluetoothError.ConnectionLostException -> {
                UIError(resources.getString(R.string.ble_connection_lost_desc), failure.code) {
                    connectToPeripheral()
                }
            }
            else -> {
                UIError(resources.getString(R.string.helium_connection_rejected)) {
                    connectToPeripheral()
                }
            }
        })
    }

    fun setSelectedDevice(scannedDevice: ScannedDevice) {
        super.scannedDevice = scannedDevice
    }

    fun setFrequency(frequency: Frequency) {
        viewModelScope.launch(dispatcher) {
            connectionUseCase.setFrequency(frequency).onRight {
                reboot()
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onBLEError.postValue(
                    UIError(resources.getString(R.string.set_frequency_failed_desc), it.code) {
                        setFrequency(frequency)
                    }
                )
            }
        }
    }

    private fun reboot() {
        viewModelScope.launch(dispatcher) {
            onRebooting.postValue(true)
            connectionUseCase.reboot().onRight {
                connectToPeripheral()
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onBLEError.postValue(
                    UIError(resources.getString(R.string.helium_reboot_failed)) {
                        reboot()
                    }
                )
            }
        }
    }

    private fun connectToPeripheral() {
        viewModelScope.launch(dispatcher) {
            onBLEConnection.postValue(true)
            delay(CONNECT_DELAY_ON_REBOOT)
            super.connect(false)
        }
    }

    private fun fetchDeviceEUI() {
        viewModelScope.launch(dispatcher) {
            connectionUseCase.fetchDeviceEUI().onRight {
                /**
                 * BLE returns Dev EUI with `:` in between so we need to unmask it
                 */
                onBLEDevEUI.postValue(it.unmask())
                fetchClaimingKey()
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onBLEError.postValue(
                    UIError(resources.getString(R.string.helium_fetching_info_failed)) {
                        fetchDeviceEUI()
                    }
                )
            }
        }
    }

    private fun fetchClaimingKey() {
        viewModelScope.launch(dispatcher) {
            connectionUseCase.fetchClaimingKey().onRight {
                onBLEClaimingKey.postValue(it)
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onBLEError.postValue(
                    UIError(resources.getString(R.string.helium_fetching_info_failed)) {
                        fetchClaimingKey()
                    }
                )
            }
        }
    }
}
