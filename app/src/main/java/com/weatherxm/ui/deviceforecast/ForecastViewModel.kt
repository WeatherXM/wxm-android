package com.weatherxm.ui.deviceforecast

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.data.ServerError
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
                    Timber.w("Getting daily forecast failed: $it")
                    when (it) {
                        is Failure.NetworkError -> onForecast.postValue(
                            Resource.error(resHelper.getString(R.string.network_error))
                        )
                        is ServerError -> onForecast.postValue(
                            Resource.error(resHelper.getString(R.string.server_error))
                        )
                        is Failure.UnknownError -> onForecast.postValue(
                            Resource.error(resHelper.getString(R.string.unknown_error))
                        )
                    }
                }
        }
    }
}
