package com.weatherxm.ui.scandevices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.ui.ScannedDevice
import com.weatherxm.usecases.ScanDevicesUseCase
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ScanDevicesViewModel : ViewModel(), KoinComponent {
    private val scanDevicesUseCase: ScanDevicesUseCase by inject()
    private val scannedDevices = mutableListOf<ScannedDevice>()

    private val onNewAdvertisement = MutableLiveData<List<ScannedDevice>>()
    fun onNewAdvertisement(): LiveData<List<ScannedDevice>> = onNewAdvertisement

    fun scanBleDevices() {
        viewModelScope.launch {
            scanDevicesUseCase.scanBleDevices()
                .collect {
                    if (!scannedDevices.contains(it)) {
                        Timber.d("New scanned device collected: $it")
                        scannedDevices.add(it)
                        this@ScanDevicesViewModel.onNewAdvertisement.postValue(scannedDevices)
                    }
                }
        }
    }
}
