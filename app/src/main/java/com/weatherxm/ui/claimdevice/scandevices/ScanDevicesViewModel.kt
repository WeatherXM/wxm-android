package com.weatherxm.ui.claimdevice.scandevices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.ui.ScannedDevice
import com.weatherxm.usecases.ScanDevicesUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ScanDevicesViewModel : ViewModel(), KoinComponent {
    private val scanDevicesUseCase: ScanDevicesUseCase by inject()
    private val resHelper: ResourcesHelper by inject()
    private lateinit var scannedDevices: MutableList<ScannedDevice>

    private val onNewAdvertisement = MutableLiveData<List<ScannedDevice>>()
    fun onNewAdvertisement(): LiveData<List<ScannedDevice>> = onNewAdvertisement

    private val onProgress = MutableLiveData<Resource<Unit>>()
    fun onProgress(): LiveData<Resource<Unit>> = onProgress

    fun scanBleDevices() {
        viewModelScope.launch {
            scanDevicesUseCase.registerOnScanningCompletionStatus()
                .collect { completionStatus ->
                    completionStatus.map {
                        onProgress.postValue(Resource.success(Unit))
                    }.mapLeft {
                        onProgress.postValue(
                            Resource.error(resHelper.getString(R.string.scan_failed_desc))
                        )
                    }
                }
        }

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

        viewModelScope.launch {
            onProgress.postValue(Resource.loading())
            scannedDevices = mutableListOf()
            scanDevicesUseCase.startScanning()
        }
    }

    fun isScanningRunning(): Boolean {
        return onProgress.value?.status == Status.LOADING
    }
}
