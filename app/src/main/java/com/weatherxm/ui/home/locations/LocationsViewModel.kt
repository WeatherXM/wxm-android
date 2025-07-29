package com.weatherxm.ui.home.locations

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.Location
import com.weatherxm.ui.common.LocationWeather
import com.weatherxm.ui.common.LocationsWeather
import com.weatherxm.ui.common.Resource
import com.weatherxm.usecases.LocationsUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class LocationsViewModel(
    private val usecase: LocationsUseCase,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _onLocationsWeather = MutableLiveData<Resource<LocationsWeather>>()
    private var savedLocations = emptyList<Location>()

    fun onLocationsWeather(): LiveData<Resource<LocationsWeather>> =
        _onLocationsWeather

    fun getSavedLocations(): List<Location> {
        savedLocations = usecase.getSavedLocations()
        return savedLocations
    }

    fun isLocationSaved(location: Location): Boolean = savedLocations.contains(location)

    fun fetch(currentLocation: Location?, isLoggedIn: Boolean) {
        _onLocationsWeather.postValue(Resource.loading())

        viewModelScope.launch(dispatcher) {
            /**
             * Fetch current location weather (if provided)
             */
            val currentLocationJob = async {
                currentLocation?.let { usecase.getLocationWeather(it) }
            }

            /**
             * Should not continue if we have an error in the current location's weather fetch.
             */
            var currentLocationWeather: LocationWeather? = null
            currentLocationJob.await()?.onRight {
                currentLocationWeather = it
            }?.onLeft {
                analytics.trackEventFailure(it.code)
                _onLocationsWeather.postValue(
                    Resource.error(it.getDefaultMessage(R.string.error_reach_out))
                )
                return@launch
            }

            /**
             * Fetch all saved locations weather in parallel, preserving order.
             *
             * If the user is NOT logged in fetch only the first (if exists) saved location.
             */
            val deferredWeathers = if (isLoggedIn) {
                savedLocations.map { location ->
                    async {
                        usecase.getLocationWeather(location).apply {
                            onLeft { analytics.trackEventFailure(it.code) }
                        }
                    }
                }
            } else if (savedLocations.isNotEmpty()) {
                listOf(
                    async {
                        usecase.getLocationWeather(savedLocations[0])
                    }
                )
            } else {
                listOf()
            }

            /**
             * Wait for the jobs to complete.
             */
            currentLocationJob.join()
            val weatherResults = deferredWeathers.awaitAll()

            /**
             * Extract the error in the saved locations fetch if it's present
             */
            val error = weatherResults.firstOrNull { it.isLeft() }?.leftOrNull()

            if (error != null) {
                _onLocationsWeather.postValue(
                    Resource.error(error.getDefaultMessage(R.string.error_reach_out))
                )
                return@launch
            } else {
                /**
                 *  All succeeded – return the weather objects in the same order as savedLocations
                 */
                _onLocationsWeather.postValue(
                    Resource.success(
                        LocationsWeather(
                            current = currentLocationWeather,
                            saved = weatherResults.mapNotNull { it.getOrNull() }
                        )
                    )
                )
            }
        }
    }

    fun clearLocationForecastFromCache() = usecase.clearLocationForecastFromCache()
}
