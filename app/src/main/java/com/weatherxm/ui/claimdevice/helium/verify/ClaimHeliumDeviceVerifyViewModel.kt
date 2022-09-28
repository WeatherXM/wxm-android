package com.weatherxm.ui.claimdevice.helium.verify

import android.bluetooth.BluetoothDevice
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.ui.UIError
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClaimHeliumDeviceVerifyViewModel : ViewModel(), KoinComponent {
    private val usecase: BluetoothConnectionUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    private val onError = MutableLiveData<UIError>()
    fun onError() = onError

    private val onPairing = MutableLiveData(true)
    fun onPairing(): LiveData<Boolean> = onPairing

    fun setPeripheral(address: String) {
        usecase.setPeripheral(address)
            .map {
                connectToPeripheral()
            }
            .mapLeft {
                onError.postValue(
                    UIError(resHelper.getString(R.string.helium_pairing_failed_desc)) {
                        setPeripheral(address)
                    }
                )
            }
    }

    private fun connectToPeripheral() {
        viewModelScope.launch {
            usecase.connectToPeripheral().mapLeft {
                onError.postValue(
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
                    })
            }
        }
    }

    fun parseScanResult(result: String?) {
        // TODO: Parse scan's result 
    }

    // TODO: Remove this
    fun update(updatePackage: Uri) {
        viewModelScope.launch {
            usecase.update(updatePackage).collect {
                // TODO: Handle progress here
            }
        }
    }

    init {
        viewModelScope.launch {
            usecase.registerOnBondStatus().collect {
                when (it) {
                    BluetoothDevice.BOND_BONDED -> {
                        onPairing.postValue(false)
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        onPairing.postValue(true)
                    }
                    BluetoothDevice.BOND_NONE -> {
                        onError.postValue(
                            UIError(resHelper.getString(R.string.helium_pairing_failed_desc)) {
                                connectToPeripheral()
                            }
                        )
                        onPairing.postValue(false)
                    }
                }
            }
        }
    }

}
