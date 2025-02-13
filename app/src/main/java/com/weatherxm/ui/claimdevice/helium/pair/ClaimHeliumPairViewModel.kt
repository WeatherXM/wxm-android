package com.weatherxm.ui.claimdevice.helium.pair

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.data.models.Failure
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.components.BluetoothHeliumViewModel
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

class ClaimHeliumPairViewModel(
    private val scanUseCase: BluetoothScannerUseCase,
    connectionUseCase: BluetoothConnectionUseCase,
    private val resources: Resources,
    analytics: AnalyticsWrapper,
    dispatcher: CoroutineDispatcher
) : BluetoothHeliumViewModel(String.empty(), null, connectionUseCase, analytics, dispatcher) {
    private var scannedDevices: MutableList<ScannedDevice> = mutableListOf()

    private val onBLEError = MutableLiveData<UIError>()
    private val onBLEConnectionLost = MutableLiveData<Boolean>()
    private val onBLEConnection = MutableLiveData<Boolean>()
    private val onNewScannedDevice = MutableLiveData<List<ScannedDevice>>()
    private val onScanStatus = MutableLiveData<Resource<Unit>>()
    private val onScanProgress = MutableLiveData<Int>()

    private var bleConnectionStarted = false

    fun onNewScannedDevice(): LiveData<List<ScannedDevice>> = onNewScannedDevice
    fun onScanStatus(): LiveData<Resource<Unit>> = onScanStatus
    fun onScanProgress(): LiveData<Int> = onScanProgress
    fun onBLEError() = onBLEError
    fun onBLEConnectionLost() = onBLEConnectionLost
    fun onBLEConnection() = onBLEConnection

    fun getSelectedDevice(): ScannedDevice = super.scannedDevice

    @Suppress("MagicNumber")
    fun scanBleDevices() {
        onScanStatus.postValue(Resource.loading())
        scannedDevices.clear()
        timer.start(
            onProgress = {
                onScanProgress.postValue(it)
            },
            onFinished = {
                onScanProgress.postValue(100)
                onScanStatus.postValue(Resource.success(Unit))
                super@ClaimHeliumPairViewModel.stopScanning()
            }
        )
        scanningJob = viewModelScope.launch(dispatcher) {
            scanUseCase.scan().collect {
                if (!scannedDevices.contains(it)) {
                    Timber.d("New scanned device collected: $it")
                    scannedDevices.add(it)
                    onNewScannedDevice.postValue(scannedDevices)
                }
            }
        }
    }

    override fun onNotPaired() {
        onBLEError.postValue(
            UIError(resources.getString(R.string.helium_pairing_failed_desc)) {
                setupBluetoothClaiming(super.scannedDevice)
            }
        )
    }

    override fun onConnected() {
        onBLEConnection.postValue(true)
    }

    override fun onConnectionFailure(failure: Failure) {
        bleConnectionStarted = false
        when (failure) {
            is BluetoothError.BluetoothDisabledException -> {
                onBLEError.postValue(
                    UIError(resources.getString(R.string.helium_bluetooth_disabled)) {
                        setupBluetoothClaiming(super.scannedDevice)
                    }
                )
            }
            is BluetoothError.ConnectionLostException -> {
                onBLEConnectionLost.postValue(true)
            }
            else -> {
                onBLEError.postValue(
                    UIError(resources.getString(R.string.helium_pairing_failed_desc)) {
                        setupBluetoothClaiming(super.scannedDevice)
                    }
                )
            }
        }
    }

    fun setupBluetoothClaiming(scannedDevice: ScannedDevice = super.scannedDevice) {
        if (bleConnectionStarted) return
        bleConnectionStarted = true

        super.stopScanning()
        super.scannedDevice = scannedDevice
        super.setPeripheralAndConnect(true)
    }
}
