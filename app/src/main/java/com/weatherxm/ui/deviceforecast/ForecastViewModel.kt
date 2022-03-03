package com.weatherxm.ui.deviceforecast

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.ApiError.UserError.InvalidFromDate
import com.weatherxm.data.ApiError.UserError.InvalidToDate
import com.weatherxm.data.Failure
import com.weatherxm.data.Failure.NetworkError
import com.weatherxm.data.Resource
import com.weatherxm.ui.ForecastData
import com.weatherxm.usecases.ForecastUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.getFormattedDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZonedDateTime

class ForecastViewModel : ViewModel(), KoinComponent {

    companion object {
        private const val DAYS_TO_FETCH = 7L
    }

    private val forecastUseCase: ForecastUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    // All charts currently visible
    private val onForecast = MutableLiveData<Resource<ForecastData>>().apply {
        value = Resource.loading()
    }

    fun onForecast(): LiveData<Resource<ForecastData>> = onForecast

    fun getWeatherForecast(deviceId: String) {
        onForecast.postValue(Resource.loading())
        CoroutineScope(Dispatchers.IO).launch {
            val fromDate = getFormattedDate(ZonedDateTime.now().toString())
            val toDate = getFormattedDate(ZonedDateTime.now().plusDays(DAYS_TO_FETCH).toString())
            forecastUseCase.getDailyForecast(deviceId, fromDate, toDate)
                .map { forecast ->
                    Timber.d("Got Forecast: $forecast")
                    onForecast.postValue(Resource.success(forecast))
                }
                .mapLeft {
                    handleFailure(it)
                }
        }
    }

    private fun handleFailure(failure: Failure) {
        onForecast.postValue(
            Resource.error(
                resHelper.getString(
                    when (failure) {
                        is InvalidFromDate, is InvalidToDate -> R.string.forecast_invalid_dates
                        is NetworkError -> R.string.network_error
                        else -> R.string.unknown_error
                    }
                )
            )
        )
    }
}
