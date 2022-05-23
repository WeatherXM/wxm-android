package com.weatherxm.ui.deviceforecast

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError.UserError.InvalidFromDate
import com.weatherxm.data.ApiError.UserError.InvalidToDate
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.data.repository.WeatherRepositoryImpl.Companion.PREFETCH_DAYS
import com.weatherxm.ui.DailyForecast
import com.weatherxm.usecases.ForecastUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessageResId
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZonedDateTime

class ForecastViewModel : ViewModel(), KoinComponent {

    private val forecastUseCase: ForecastUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    // All charts currently visible
    private val onForecast = MutableLiveData<Resource<List<DailyForecast>>>().apply {
        value = Resource.loading()
    }

    fun onForecast(): LiveData<Resource<List<DailyForecast>>> = onForecast

    fun getWeatherForecast(deviceId: String, forceRefresh: Boolean = false) {
        onForecast.postValue(Resource.loading())
        viewModelScope.launch {
            val fromDate = ZonedDateTime.now()
            val toDate = ZonedDateTime.now().plusDays(PREFETCH_DAYS)
            forecastUseCase.getDailyForecast(deviceId, fromDate, toDate, forceRefresh)
                .map { forecast ->
                    Timber.d("Got daily forecast from $fromDate to $toDate")
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
                        is InvalidFromDate, is InvalidToDate -> {
                            R.string.error_forecast_generic_message
                        }
                        else -> failure.getDefaultMessageResId()
                    }
                )
            )
        )
    }
}
