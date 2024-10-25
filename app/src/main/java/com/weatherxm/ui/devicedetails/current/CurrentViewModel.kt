package com.weatherxm.ui.devicedetails.current

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.models.NetworkError.NoConnectionError
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.usecases.DeviceDetailsUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class CurrentViewModel(
    var device: UIDevice = UIDevice.empty(),
    private val resources: Resources,
    private val deviceDetailsUseCase: DeviceDetailsUseCase,
    private val analytics: AnalyticsWrapper
) : ViewModel() {
    private val onDevice = MutableLiveData<UIDevice>()
    private val onLoading = MutableLiveData<Boolean>()
    private val onError = MutableLiveData<UIError>()

    fun onDevice(): LiveData<UIDevice> = onDevice
    fun onLoading(): LiveData<Boolean> = onLoading
    fun onError(): LiveData<UIError> = onError

    fun fetchDevice() {
        onLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            deviceDetailsUseCase.getDevice(device).onRight {
                Timber.d("Got Device: ${it.name}")
                device = it
                onDevice.postValue(device)
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onError.postValue(
                    when (it) {
                        is ApiError.DeviceNotFound -> {
                            UIError(resources.getString(R.string.error_device_not_found))
                        }
                        is NoConnectionError, is ConnectionTimeoutError -> {
                            UIError(
                                it.getDefaultMessage(R.string.error_reach_out_short),
                                errorCode = null,
                                ::fetchDevice
                            )
                        }
                        else -> {
                            UIError(resources.getString(R.string.error_reach_out_short))
                        }
                    }
                )
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
