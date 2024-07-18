package com.weatherxm.ui.claimdevice.helium.pair

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.components.BluetoothHeliumViewModel
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch
import timber.log.Timber

class ClaimHeliumPairViewModel(
    private val resources: Resources,
    analytics: AnalyticsWrapper,
    private val scanUseCase: BluetoothScannerUseCase,
    connectionUseCase: BluetoothConnectionUseCase
) : BluetoothHeliumViewModel(String.empty(), null, connectionUseCase, analytics) {
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
    override var timer = object : CountDownTimer(SCAN_DURATION, SCAN_COUNTDOWN_INTERVAL) {
        override fun onTick(msUntilDone: Long) {
            val progress = ((SCAN_DURATION - msUntilDone) * 100L / SCAN_DURATION).toInt()
            Timber.d("Scanning progress: $progress")
            onScanProgress.postValue(progress)
        }

        override fun onFinish() {
            super@ClaimHeliumPairViewModel.stopScanning()
            onScanProgress.postValue(100)
            onScanStatus.postValue(Resource.success(Unit))
        }
    }

    @Suppress("MagicNumber")
    fun scanBleDevices() {
        onScanStatus.postValue(Resource.loading())
        scannedDevices.clear()
        scanningJob = viewModelScope.launch {
            timer.start()
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
        analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
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

        viewModelScope.launch {
            super.setPeripheralAndConnect(true)
        }
    }
}
