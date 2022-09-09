package com.weatherxm.ui.home

import android.bluetooth.BluetoothDevice
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.Peripheral
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

    private val onConnectedDevice = MutableLiveData<Peripheral>()
    fun onConnectedDevice() = onConnectedDevice

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
                .map {
                    onConnectedDevice.postValue(it)
                }
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
}
