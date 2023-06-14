package com.weatherxm.ui.userdevice.forecast

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.NetworkError.NoConnectionError
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.usecases.UserDeviceUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ForecastViewModel(var device: Device) : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val userDeviceUseCase: UserDeviceUseCase by inject()
    private val analytics: Analytics by inject()

    private val onLoading = MutableLiveData<Boolean>()

    private val onError = MutableLiveData<UIError>()

    private val onForecast = MutableLiveData<List<UIForecast>>()

    fun onLoading(): LiveData<Boolean> = onLoading

    fun onError(): LiveData<UIError> = onError

    fun onForecast(): LiveData<List<UIForecast>> = onForecast

    fun fetchForecast(forceRefresh: Boolean = false) {
        onLoading.postValue(true)
        viewModelScope.launch {
            userDeviceUseCase.getForecast(device, forceRefresh)
                .map {
                    Timber.d("Got forecast $it")
                    if (it.isEmpty()) {
                        onError.postValue(UIError(resHelper.getString(R.string.forecast_empty)))
                    }
                    onForecast.postValue(it)
                }
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    handleForecastFailure(it)
                }
            onLoading.postValue(false)
        }
    }

    private fun handleForecastFailure(failure: Failure) {
        onError.postValue(
            when (failure) {
                is ApiError.UserError.InvalidFromDate, is ApiError.UserError.InvalidToDate -> {
                    UIError(resHelper.getString(R.string.error_forecast_generic_message))
                }
                is NoConnectionError, is ConnectionTimeoutError -> {
                    UIError(failure.getDefaultMessage(R.string.error_reach_out_short)) {
                        fetchForecast()
                    }
                }
                else -> {
                    UIError(resHelper.getString(R.string.error_reach_out_short))
                }
            }
        )
    }
}
