package com.weatherxm.ui.devicedetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Device
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.usecases.DeviceDetailsUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.RefreshHandler
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class DeviceDetailsViewModel(
    var device: Device = Device.empty(),
    var cellDevice: UIDevice = UIDevice.empty(),
    var isUserDevice: Boolean
) : ViewModel(), KoinComponent {
    companion object {
        private const val REFRESH_INTERVAL_SECONDS = 30L
    }

    private val deviceDetailsUseCase: DeviceDetailsUseCase by inject()
    private val analytics: Analytics by inject()
    private val refreshHandler = RefreshHandler(
        refreshIntervalMillis = TimeUnit.SECONDS.toMillis(REFRESH_INTERVAL_SECONDS)
    )

    private val onUnitPreferenceChanged = MutableLiveData(false)
    private val address = MutableLiveData<String?>()

    fun onUnitPreferenceChanged(): LiveData<Boolean> = onUnitPreferenceChanged
    fun address(): LiveData<String?> = address

    suspend fun deviceAutoRefresh() = refreshHandler.flow()
        .map {
            deviceDetailsUseCase.getUserDevice(device.id)
                .onRight {
                    Timber.d("Got User Device using polling: ${it.name}")
                }.onLeft {
                    analytics.trackEventFailure(it.code)
                }
        }

    fun fetchAddressFromCell() {
        if (!cellDevice.address.isNullOrEmpty()) {
            address.postValue(cellDevice.address)
            return
        }
        viewModelScope.launch {
            cellDevice.cellCenter?.let {
                address.postValue(
                    deviceDetailsUseCase.getAddressOfCell(
                        UICell(cellDevice.cellIndex, it)
                    )
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            deviceDetailsUseCase.getUnitPreferenceChangedFlow()
                .collect {
                    Timber.d("Unit preference key changed: $it. Triggering data update.")
                    onUnitPreferenceChanged.postValue(true)
                }
        }
    }
}
