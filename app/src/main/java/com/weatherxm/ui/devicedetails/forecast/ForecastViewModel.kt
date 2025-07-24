package com.weatherxm.ui.devicedetails.forecast

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.models.NetworkError.NoConnectionError
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.usecases.ForecastUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

class ForecastViewModel(
    var device: UIDevice = UIDevice.empty(),
    private val resources: Resources,
    private val forecastUseCase: ForecastUseCase,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val onLoading = MutableLiveData<Boolean>()

    private val onError = MutableLiveData<UIError>()

    private val onForecast = MutableLiveData<UIForecast>()

    fun onLoading(): LiveData<Boolean> = onLoading

    fun onError(): LiveData<UIError> = onError

    fun onForecast(): LiveData<UIForecast> = onForecast

    fun fetchForecast(forceRefresh: Boolean = false) {
        /**
         * If we got here directly from a search result or through a notification,
         * then we need to wait for the View Model to load the device from the network,
         * and then proceed in fetching the forecast because the timezone property is null otherwise
         *
         * Or do not fetch forecast at all if this device is UNFOLLOWED
         */
        if (device.isEmpty() || device.isDeviceFromSearchResult || device.isUnfollowed()) {
            return
        }
        onLoading.postValue(true)
        viewModelScope.launch(dispatcher) {
            forecastUseCase.getDeviceForecast(device, forceRefresh).onRight {
                Timber.d("Got forecast")
                if (it.isEmpty()) {
                    onError.postValue(UIError(resources.getString(R.string.forecast_empty)))
                }
                onForecast.postValue(it)
            }.onLeft {
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
                    UIError(resources.getString(R.string.error_forecast_generic_message))
                }
                is ApiError.UserError.InvalidTimezone -> {
                    UIError(resources.getString(R.string.error_forecast_invalid_timezone))
                }
                is NoConnectionError, is ConnectionTimeoutError -> {
                    UIError(failure.getDefaultMessage(R.string.error_reach_out_short)) {
                        fetchForecast()
                    }
                }
                else -> {
                    UIError(resources.getString(R.string.error_reach_out_short))
                }
            }
        )
    }
}
