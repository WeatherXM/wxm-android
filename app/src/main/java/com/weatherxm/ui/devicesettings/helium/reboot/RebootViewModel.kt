package com.weatherxm.ui.devicesettings.helium.reboot

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.data.models.Failure
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.components.BluetoothHeliumViewModel
import com.weatherxm.ui.devicesettings.RebootState
import com.weatherxm.ui.devicesettings.RebootStatus
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.util.Failure.getCode
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class RebootViewModel(
    val device: UIDevice,
    connectionUseCase: BluetoothConnectionUseCase,
    scanUseCase: BluetoothScannerUseCase,
    private val resources: Resources,
    analytics: AnalyticsWrapper,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BluetoothHeliumViewModel(
    device.getLastCharsOfLabel(),
    scanUseCase,
    connectionUseCase,
    analytics,
    dispatcher
) {

    private val onStatus = MutableLiveData<Resource<RebootState>>()
    fun onStatus() = onStatus

    override fun onScanFailure(failure: Failure) {
        onStatus.postValue(
            if (failure == BluetoothError.DeviceNotFound) {
                Resource.error(
                    resources.getString(R.string.station_not_in_range_subtitle),
                    RebootState(RebootStatus.SCAN_FOR_STATION, BluetoothError.DeviceNotFound)
                )
            } else {
                Resource.error(String.empty(), RebootState(RebootStatus.SCAN_FOR_STATION))
            }
        )
    }

    override fun onNotPaired() {
        onStatus.postValue(Resource.error(String.empty(), RebootState(RebootStatus.PAIR_STATION)))
    }

    override fun onConnected() {
        reboot()
    }

    override fun onConnectionFailure(failure: Failure) {
        onStatus.postValue(
            Resource.error(failure.getCode(), RebootState(RebootStatus.CONNECT_TO_STATION))
        )
    }

    fun startConnectionProcess() {
        onStatus.postValue(Resource.loading(RebootState(RebootStatus.CONNECT_TO_STATION)))
        super.scanAndConnect()
    }

    private fun reboot() {
        viewModelScope.launch(dispatcher) {
            onStatus.postValue(Resource.loading(RebootState(RebootStatus.REBOOTING)))
            connectionUseCase.reboot().onRight {
                onStatus.postValue(Resource.success(RebootState(RebootStatus.REBOOTING)))
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onStatus.postValue(
                    Resource.error(it.getCode(), RebootState(RebootStatus.REBOOTING))
                )
            }
        }
    }
}
