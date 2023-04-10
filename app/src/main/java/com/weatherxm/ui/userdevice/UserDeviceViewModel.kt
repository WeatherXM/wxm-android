package com.weatherxm.ui.userdevice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Device
import com.weatherxm.usecases.UserDeviceUseCase
import com.weatherxm.util.RefreshHandler
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class UserDeviceViewModel(var device: Device) : ViewModel(), KoinComponent {
    companion object {
        private const val REFRESH_INTERVAL_SECONDS = 30L
    }

    private val userDeviceUseCase: UserDeviceUseCase by inject()
    private val refreshHandler = RefreshHandler(
        refreshIntervalMillis = TimeUnit.SECONDS.toMillis(REFRESH_INTERVAL_SECONDS)
    )

    private val onUnitPreferenceChanged = MutableLiveData(false)

    fun onUnitPreferenceChanged(): LiveData<Boolean> = onUnitPreferenceChanged

    suspend fun deviceAutoRefresh() = refreshHandler.flow()
        .map {
            userDeviceUseCase.getUserDevice(device.id)
                .onRight {
                    Timber.d("Got User Device using polling: ${it.name}")
                }
        }

    init {
        viewModelScope.launch {
            userDeviceUseCase.getUnitPreferenceChangedFlow()
                .collect {
                    Timber.d("Unit preference key changed: $it. Triggering data update.")
                    onUnitPreferenceChanged.postValue(true)
                }
        }
    }
}
