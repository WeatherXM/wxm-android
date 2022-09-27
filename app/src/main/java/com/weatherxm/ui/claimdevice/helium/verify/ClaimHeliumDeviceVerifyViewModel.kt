package com.weatherxm.ui.claimdevice.helium.verify

import android.bluetooth.BluetoothDevice
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
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
            usecase.registerOnBondStatus().collect {
                when (it) {
                    BluetoothDevice.BOND_BONDED -> {
                        // TODO: Do what? Fetch device key?
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        // TODO: Do what? Show progress (if any??)?
                    }
                    BluetoothDevice.BOND_NONE -> {
                        onError.postValue(
                            UIError(resHelper.getString(R.string.helium_pairing_failed_desc)) {
                                connectToPeripheral()
                            }
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            usecase.connectToPeripheral()
                .mapLeft {
                    // TODO: Handle failure
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

}
