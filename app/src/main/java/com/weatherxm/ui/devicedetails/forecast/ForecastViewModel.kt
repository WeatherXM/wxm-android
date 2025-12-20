package com.weatherxm.ui.devicedetails.forecast

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.models.NetworkError.NoConnectionError
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.usecases.ForecastUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

class ForecastViewModel(
    var device: UIDevice = UIDevice.empty(),
    private val resources: Resources,
    private val forecastUseCase: ForecastUseCase,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val onDefaultForecast = MutableLiveData<Resource<UIForecast>>()
    private val onPremiumForecast = MutableLiveData<Resource<UIForecast>>()

    fun onDefaultForecast(): LiveData<Resource<UIForecast>> = onDefaultForecast
    fun onPremiumForecast(): LiveData<Resource<UIForecast>> = onPremiumForecast

    fun fetchForecasts(forceRefresh: Boolean = false) {
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
        fetchDeviceForecast(
            mutableLiveData = onDefaultForecast,
            fetchOperation = { forecastUseCase.getDeviceDefaultForecast(device, forceRefresh) }
        )
        // TODO: STOPSHIP: We need a check here to not fetch the below if not premium available.
        fetchDeviceForecast(
            mutableLiveData = onPremiumForecast,
            fetchOperation = { forecastUseCase.getDevicePremiumForecast(device) }
        )
    }

    private fun fetchDeviceForecast(
        mutableLiveData: MutableLiveData<Resource<UIForecast>>,
        fetchOperation: suspend () -> Either<Failure, UIForecast>
    ) {
        viewModelScope.launch(dispatcher) {
            mutableLiveData.postValue(Resource.loading())
            fetchOperation().onRight {
                if (it.isEmpty()) {
                    mutableLiveData.postValue(
                        Resource.error(resources.getString(R.string.forecast_empty))
                    )
                } else {
                    mutableLiveData.postValue(Resource.success(it))
                }
            }.onLeft {
                analytics.trackEventFailure(it.code)
                mutableLiveData.postValue(Resource.error(getFailureMessage(it)))
            }
        }
    }

    private fun getFailureMessage(failure: Failure): String {
        return when (failure) {
            is ApiError.UserError.InvalidFromDate, is ApiError.UserError.InvalidToDate -> {
                resources.getString(R.string.error_forecast_generic_message)
            }
            is ApiError.UserError.InvalidTimezone -> {
                resources.getString(R.string.error_forecast_invalid_timezone)
            }
            is NoConnectionError, is ConnectionTimeoutError -> {
                failure.getDefaultMessage(R.string.error_reach_out_short)
            }
            else -> {
                resources.getString(R.string.error_reach_out_short)
            }
        }
    }
}
