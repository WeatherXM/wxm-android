package com.weatherxm.ui.userdevice.current

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError
import com.weatherxm.data.Device
import com.weatherxm.data.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.NetworkError.NoConnectionError
import com.weatherxm.ui.common.UIError
import com.weatherxm.usecases.UserDeviceUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class CurrentViewModel(var device: Device) : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val userDeviceUseCase: UserDeviceUseCase by inject()

    private val onDevice = MutableLiveData<Device>()

    private val onLoading = MutableLiveData<Boolean>()

    private val onError = MutableLiveData<UIError>()

    fun onDevice(): LiveData<Device> = onDevice

    fun onLoading(): LiveData<Boolean> = onLoading

    fun onError(): LiveData<UIError> = onError

    fun fetchUserDevice() {
        onLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            userDeviceUseCase.getUserDevice(device.id)
                .map {
                    Timber.d("Got User Device: ${it.name}")
                    device = it
                    onDevice.postValue(device)
                }
                .mapLeft {
                    when (it) {
                        is ApiError.DeviceNotFound -> {
                            UIError(resHelper.getString(R.string.error_user_device_not_found))
                        }
                        is NoConnectionError, is ConnectionTimeoutError -> {
                            UIError(
                                it.getDefaultMessage(R.string.error_reach_out_short),
                                errorCode = null,
                                ::fetchUserDevice
                            )
                        }
                        else -> {
                            UIError(resHelper.getString(R.string.error_reach_out_short))
                        }
                    }
                }
                .onLeft {
                    onError.postValue(it)
                }
            onLoading.postValue(false)
        }
    }

    init {
        /**
         * Because we observe the device updates from the Activity, but we have the "Loading" state
         * in the Fragment, we want the progress bar to be visible the first time the user visits
         * this screen until the device updates correctly. Therefore we need the following line.
         */
        onLoading.postValue(true)
    }
}
