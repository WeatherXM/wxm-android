package com.weatherxm.ui.devicedetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.usecases.AuthUseCase
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
    var device: UIDevice = UIDevice.empty(),
    var openExplorerOnBack: Boolean
) : ViewModel(), KoinComponent {
    companion object {
        private const val REFRESH_INTERVAL_SECONDS = 30L
    }

    private val deviceDetailsUseCase: DeviceDetailsUseCase by inject()
    private val authUseCase: AuthUseCase by inject()
    private val analytics: Analytics by inject()

    private val refreshHandler = RefreshHandler(
        refreshIntervalMillis = TimeUnit.SECONDS.toMillis(REFRESH_INTERVAL_SECONDS)
    )

    private val onUnitPreferenceChanged = MutableLiveData(false)
    private val address = MutableLiveData<String?>()
    private var isLoggedIn: Boolean? = null
    private val onDevicePolling = MutableLiveData<UIDevice>()

    fun onDevicePolling(): LiveData<UIDevice> = onDevicePolling
    fun onUnitPreferenceChanged(): LiveData<Boolean> = onUnitPreferenceChanged
    fun address(): LiveData<String?> = address

    fun isLoggedIn() = isLoggedIn

    suspend fun deviceAutoRefresh() = refreshHandler.flow()
        .map {
            deviceDetailsUseCase.getUserDevice(device)
                .onRight {
                    Timber.d("Got Device using polling: ${it.name}")
                    onDevicePolling.postValue(it)
                }.onLeft {
                    analytics.trackEventFailure(it.code)
                }
        }

    fun fetchAddressFromCell() {
        if (!device.address.isNullOrEmpty()) {
            address.postValue(device.address)
            return
        }
        viewModelScope.launch {
            device.cellCenter?.let {
                address.postValue(
                    deviceDetailsUseCase.getAddressOfCell(
                        UICell(device.cellIndex, it)
                    )
                )
            }
        }
    }

    fun createNormalizedName(): String {
        return if (!device.isEmpty()) {
            device.toNormalizedName()
        } else {
            ""
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
        viewModelScope.launch {
            isLoggedIn = authUseCase.isLoggedIn().getOrElse { false }
        }
    }
}
