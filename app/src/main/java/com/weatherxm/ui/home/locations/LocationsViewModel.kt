package com.weatherxm.ui.home.locations

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.Location
import com.weatherxm.ui.common.LocationWeather
import com.weatherxm.ui.common.UIError
import com.weatherxm.usecases.LocationWeatherUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

class LocationsViewModel(
    private val usecase: LocationWeatherUseCase,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _onLoading = MutableLiveData<Boolean>()
    private val _onError = MutableLiveData<UIError?>()
    private val _onLocationWeather = MutableLiveData<LocationWeather>()

    fun onLoading(): LiveData<Boolean> = _onLoading
    fun onError(): LiveData<UIError?> = _onError
    fun onLocationWeather(): LiveData<LocationWeather> = _onLocationWeather

    fun fetch(currentLocation: Location?) {
        _onError.postValue(null)
        viewModelScope.launch(dispatcher) {
            val currentLocationJob = launch {
                currentLocation?.let { location ->
                    _onLoading.postValue(true)
                    usecase.getLocationWeather(location.lat, location.lon).onRight {
                        Timber.d("Got weather for current location")
                        _onLocationWeather.postValue(it)
                    }.onLeft {
                        analytics.trackEventFailure(it.code)
                        _onError.postValue(
                            UIError(
                                it.getDefaultMessage(R.string.error_reach_out),
                                errorCode = null,
                            ) {
                                fetch(currentLocation)
                            }
                        )
                        return@launch
                    }
                }
            }

            // TODO: STOPSHIP: Fetch for saved locations the weather here and use `join` at the end.
            currentLocationJob.join()
            _onLoading.postValue(false)
        }
    }

}
