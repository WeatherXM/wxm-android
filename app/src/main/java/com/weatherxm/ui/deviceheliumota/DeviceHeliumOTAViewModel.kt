package com.weatherxm.ui.deviceheliumota

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.data.models.BluetoothOTAState
import com.weatherxm.data.models.Failure
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.components.BluetoothHeliumViewModel
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.usecases.BluetoothUpdaterUseCase
import com.weatherxm.util.Failure.getCode
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
class DeviceHeliumOTAViewModel(
    val device: UIDevice,
    private val deviceIsBleConnected: Boolean,
    private val resources: Resources,
    private val updaterUseCase: BluetoothUpdaterUseCase,
    connectionUseCase: BluetoothConnectionUseCase,
    scanUseCase: BluetoothScannerUseCase,
    analytics: AnalyticsWrapper
) : BluetoothHeliumViewModel(
    device.getLastCharsOfLabel(),
    scanUseCase,
    connectionUseCase,
    analytics
) {
    private val onStatus = MutableLiveData<Resource<State>>()
    fun onStatus() = onStatus

    private val onInstallingProgress = MutableLiveData<Int>()
    fun onInstallingProgress() = onInstallingProgress

    override fun onScanFailure(failure: Failure) {
        onStatus.postValue(
            if (failure == BluetoothError.DeviceNotFound) {
                Resource.error(
                    resources.getString(R.string.station_not_in_range_subtitle),
                    State(OTAStatus.SCAN_FOR_STATION, BluetoothError.DeviceNotFound)
                )
            } else {
                Resource.error(String.empty(), State(OTAStatus.SCAN_FOR_STATION))
            }
        )
    }

    override fun onNotPaired() {
        onStatus.postValue(Resource.error(String.empty(), State(OTAStatus.PAIR_STATION)))
    }

    override fun onConnected() {
        downloadFirmwareAndGetFileURI()
    }

    override fun onConnectionFailure(failure: Failure) {
        onStatus.postValue(Resource.error(failure.getCode(), State(OTAStatus.CONNECT_TO_STATION)))
    }

    fun startConnectionProcess() {
        /**
         * If device is already connected we have no reason to BLE Scan again so we go directly
         * to downloading and installing.
         * But we want to launch this from here as when we get here it means that all necessary
         * checks on bluetooth are successful.
         */
        if (deviceIsBleConnected) {
            downloadFirmwareAndGetFileURI()
            return
        }
        onStatus.postValue(Resource.loading(State(OTAStatus.CONNECT_TO_STATION)))
        super.scanAndConnect()
    }

    private fun downloadFirmwareAndGetFileURI() {
        onStatus.postValue(Resource.loading(State(OTAStatus.DOWNLOADING)))
        viewModelScope.launch {
            updaterUseCase.downloadFirmwareAndGetFileURI(device.id).onRight {
                update(it)
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onStatus.postValue(
                    Resource.error(
                        resources.getString(R.string.error_helium_ota_download_failed),
                        State(OTAStatus.DOWNLOADING)
                    )
                )
            }
        }
    }

    fun update(uri: Uri) {
        viewModelScope.launch {
            onStatus.postValue(Resource.loading(State(OTAStatus.INSTALLING)))
            updaterUseCase.update(uri).collect {
                when (it.state) {
                    BluetoothOTAState.IN_PROGRESS -> {
                        onInstallingProgress.postValue(it.progress)
                    }
                    BluetoothOTAState.COMPLETED -> {
                        onStatus.postValue(Resource.success(State(OTAStatus.INSTALLING)))
                        device.assignedFirmware?.let { versionInstalled ->
                            updaterUseCase.onUpdateSuccess(device.id, versionInstalled)
                        }
                    }
                    BluetoothOTAState.ABORTED -> {
                        onStatus.postValue(
                            Resource.error(
                                resources.getString(R.string.error_helium_ota_aborted),
                                State(OTAStatus.INSTALLING)
                            )
                        )
                    }
                    BluetoothOTAState.FAILED -> {
                        analytics.trackEventFailure(Failure.CODE_BL_OTA_FAILED)
                        onStatus.postValue(
                            Resource.error(
                                String.empty(),
                                State(
                                    OTAStatus.INSTALLING,
                                    otaError = it.error,
                                    otaErrorType = it.errorType,
                                    otaErrorMessage = it.message
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}
