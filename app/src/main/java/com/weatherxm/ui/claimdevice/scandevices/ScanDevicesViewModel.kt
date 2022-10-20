package com.weatherxm.ui.claimdevice.scandevices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.usecases.BluetoothScannerUseCase
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ScanDevicesViewModel : ViewModel(), KoinComponent {
    private val scanDevicesUseCase: BluetoothScannerUseCase by inject()
    private var scannedDevices: MutableList<ScannedDevice> = mutableListOf()

    private val onNewAdvertisement = MutableLiveData<List<ScannedDevice>>()
    fun onNewAdvertisement(): LiveData<List<ScannedDevice>> = onNewAdvertisement

    private val onProgress = MutableLiveData<Resource<Unit>>()
    fun onProgress(): LiveData<Resource<Unit>> = onProgress

    fun scanBleDevices() {
        viewModelScope.launch {
            onProgress.postValue(Resource.loading())
            scannedDevices.clear()
            scanDevicesUseCase.startScanning()
                .map {
                    onProgress.postValue(Resource.success(Unit))
                }
                .mapLeft {
                    onProgress.postValue(Resource.error(""))
                }
        }
    }

    fun isScanningRunning(): Boolean {
        return onProgress.value?.status == Status.LOADING
    }

    init {
        viewModelScope.launch {
            scanDevicesUseCase.registerOnScanning()
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
