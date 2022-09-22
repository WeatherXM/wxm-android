package com.weatherxm.ui.home

import android.bluetooth.BluetoothDevice
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.ui.ScannedDevice
import com.weatherxm.usecases.BluetoothConnectionUseCase
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HomeViewModel : ViewModel(), KoinComponent {
    private val usecase: BluetoothConnectionUseCase by inject()

    private val onScannedDeviceSelected = MutableLiveData<ScannedDevice>()
    fun onScannedDeviceSelected(): LiveData<ScannedDevice> = onScannedDeviceSelected

    private val onClaimManually = MutableLiveData(false)
    fun onClaimManually() = onClaimManually

    private val onBondedDevice = MutableLiveData<Unit>()
    fun onBondedDevice() = onBondedDevice

    fun selectScannedDevice(scannedDevice: ScannedDevice) {
        onScannedDeviceSelected.postValue(scannedDevice)
    }

    fun claimManually() {
        onClaimManually.postValue(true)
    }

    fun setPeripheral(bluetoothDevice: BluetoothDevice) {
        usecase.setPeripheral(bluetoothDevice)
    }

    fun connectToPeripheral() {
        viewModelScope.launch {
            usecase.connectToPeripheral()
                .mapLeft {
                    // TODO: Handle failure
                }
        }
    }

    fun update(updatePackage: Uri) {
        viewModelScope.launch {
            usecase.update(updatePackage).collect {
                // TODO: Handle progress here
            }
        }
    }

    init {
        viewModelScope.launch {
            usecase.registerOnBondStatus()
                .collect {
                    when (it) {
                        BluetoothDevice.BOND_BONDING -> {
                            // TODO: What to show here??
                        }
                        BluetoothDevice.BOND_BONDED -> {
                            onBondedDevice.postValue(Unit)
                        }
                        BluetoothDevice.BOND_NONE -> {
                            // TODO: What to show here??
                        }
                    }
                }
        }
    }
}
