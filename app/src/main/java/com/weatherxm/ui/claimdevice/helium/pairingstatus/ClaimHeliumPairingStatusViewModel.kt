package com.weatherxm.ui.claimdevice.helium.pairingstatus

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.UIError
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClaimHeliumPairingStatusViewModel : ViewModel(), KoinComponent {
    private val usecase: BluetoothConnectionUseCase by inject()
    private val resHelper: ResourcesHelper by inject()
    private val onPairing = MutableLiveData<Resource<String>>().apply {
        value = Resource.loading()
    }
    private val onBLEPaired = MutableLiveData(false)
    private val onBLEError = MutableLiveData<UIError>()
    private val onBLEDevEUI = MutableLiveData<String>()

    fun onPairing() = onPairing
    fun onBLEPaired() = onBLEPaired
    fun onBLEError() = onBLEError
    fun onBLEDevEUI() = onBLEDevEUI

    fun pair(devEUI: String, deviceKey: String) {
        viewModelScope.launch {
            // TODO: Actual API call
            delay(5000L)
            onPairing.postValue(Resource.success(null))
        }
    }

    fun setupBluetoothClaiming(macAddress: String) {
        usecase.getDeviceEUI(macAddress)?.let {
            onBLEDevEUI.postValue(it)
            setPeripheral(macAddress)
        } ?: run {
            onBLEError.postValue(
                UIError(resHelper.getString(R.string.helium_pairing_failed_desc)) {
                    setupBluetoothClaiming(macAddress)
                })
        }
    }

    private fun setPeripheral(macAddress: String) {
        usecase.setPeripheral(macAddress)
            .map {
                connectToPeripheral()
            }
            .mapLeft {
                onBLEError.postValue(UIError(resHelper.getString(R.string.helium_pairing_failed_desc)) {
                    setPeripheral(macAddress)
                })
            }
    }

    private fun connectToPeripheral() {
        viewModelScope.launch {
            usecase.connectToPeripheral().mapLeft {
                onBLEError.postValue(
                    when (it) {
                        is BluetoothError.BluetoothDisabledException -> {
                            UIError(resHelper.getString(R.string.helium_bluetooth_disabled)) {
                                connectToPeripheral()
                            }
                        }
                        is BluetoothError.ConnectionLostException -> {
                            UIError(resHelper.getString(R.string.helium_bluetooth_connection_lost)) {
                                connectToPeripheral()
                            }
                        }
                        else -> {
                            UIError(resHelper.getString(R.string.helium_pairing_failed_desc)) {
                                connectToPeripheral()
                            }
                        }
                    }
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            usecase.registerOnBondStatus().collect {
                when (it) {
                    BluetoothDevice.BOND_BONDED -> {
                        onBLEPaired.postValue(true)
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        onBLEPaired.postValue(false)
                    }
                    BluetoothDevice.BOND_NONE -> {
                        onBLEError.postValue(
                            UIError(resHelper.getString(R.string.helium_pairing_failed_desc)) {
                                connectToPeripheral()
                            }
                        )
                    }
                }
            }
        }
    }
}
