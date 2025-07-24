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
import com.weatherxm.usecases.LocationsUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

class LocationsViewModel(
    private val usecase: LocationsUseCase,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _onLoading = MutableLiveData<Boolean>().apply {
        value = true
    }
    private val _onError = MutableLiveData<UIError?>()
    private val _onLocationWeather = MutableLiveData<LocationWeather>()
    private var savedLocations = emptyList<Location>()

    fun onLoading(): LiveData<Boolean> = _onLoading
    fun onError(): LiveData<UIError?> = _onError
    fun onLocationWeather(): LiveData<LocationWeather> = _onLocationWeather

    fun getSavedLocations(): List<Location> {
        savedLocations = usecase.getSavedLocations()
        return savedLocations
    }

    fun isLocationSaved(location: Location): Boolean = savedLocations.contains(location)

    fun fetch(currentLocation: Location?, isLoggedIn: Boolean) {
        _onError.postValue(null)
        viewModelScope.launch(dispatcher) {
            _onLoading.postValue(true)
            val currentLocationJob = launch {
                currentLocation?.let { location ->
                    usecase.getLocationWeather(location).onRight {
                        Timber.d("Got weather for current location")
                        _onLocationWeather.postValue(it)
                    }.onLeft {
                        analytics.trackEventFailure(it.code)
                        _onError.postValue(
                            UIError(
                                it.getDefaultMessage(R.string.error_reach_out),
                                errorCode = null,
                            ) {
                                fetch(currentLocation, isLoggedIn)
                            }
                        )
                        return@launch
                    }
                }
            }

            // TODO: STOPSHIP: Fetch for saved locations the weather here and use `join` at the end.
            // TODO: STOPSHIP: If user is logged out fetch max 1 saved location otherwise max 10.
            currentLocationJob.join()
            _onLoading.postValue(false)
        }
    }

    fun clearLocationForecastFromCache() = usecase.clearLocationForecastFromCache()
    fun onNoLocationPermission() = _onLoading.postValue(false)
}
