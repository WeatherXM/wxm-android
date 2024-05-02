package com.weatherxm.ui.claimdevice.location

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.annotation.RequiresPermission
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.data.Location
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.components.BaseMapFragment.Companion.REVERSE_GEOCODING_DELAY
import com.weatherxm.usecases.EditLocationUseCase
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.util.LocationHelper
import com.weatherxm.util.Validator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class ClaimLocationViewModel(
    private val editLocationUseCase: EditLocationUseCase,
    private val analytics: AnalyticsWrapper,
    private val locationHelper: LocationHelper
) : ViewModel() {
    private var reverseGeocodingJob: Job? = null
    private var installationLocation = Location(0.0, 0.0)
    private var deviceType = DeviceType.M5_WIFI

    private val onRequestUserLocation = MutableLiveData(false)
    private val onMoveToLocation = MutableLiveData<Location?>()
    private val onSearchResults = MutableLiveData<List<SearchSuggestion>?>(mutableListOf())
    private val onReverseGeocodedAddress = MutableLiveData<String?>(null)

    fun onRequestUserLocation() = onRequestUserLocation
    fun onMoveToLocation() = onMoveToLocation
    fun onSearchResults() = onSearchResults
    fun onReverseGeocodedAddress() = onReverseGeocodedAddress

    fun requestUserLocation() {
        onRequestUserLocation.postValue(true)
    }

    fun setDeviceType(type: DeviceType) {
        deviceType = type
    }

    fun getDeviceType(): DeviceType {
        return deviceType
    }

    fun validateLocation(lat: Double, lon: Double): Boolean {
        return Validator.validateLocation(lat, lon)
    }

    fun setInstallationLocation(lat: Double, lon: Double) {
        installationLocation.lat = lat
        installationLocation.lon = lon
    }

    fun getInstallationLocation(): Location {
        return installationLocation
    }

    @RequiresPermission(anyOf = [ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION])
    fun getLocation() {
        locationHelper.getLocationAndThen {
            onMoveToLocation.postValue(it)
        }
    }

    fun geocoding(query: String) {
        viewModelScope.launch {
            editLocationUseCase.getSearchSuggestions(query)
                .onRight { suggestions ->
                    onSearchResults.postValue(suggestions)
                }
                .onLeft {
                    onSearchResults.postValue(null)
                }
        }
    }

    fun getLocationFromSearchSuggestion(suggestion: SearchSuggestion) {
        viewModelScope.launch {
            editLocationUseCase.getSuggestionLocation(suggestion)
                .onRight { location ->
                    onMoveToLocation.postValue(location)
                }
                .onLeft {
                    analytics.trackEventFailure(it.code)
                }
        }
    }

    fun getAddressFromPoint(point: Point?) {
        if (point == null) {
            return
        }
        reverseGeocodingJob?.let {
            if (it.isActive) {
                it.cancel("Cancelling running reverse geocoding job.")
            }
        }

        reverseGeocodingJob = viewModelScope.launch {
            delay(REVERSE_GEOCODING_DELAY)
            editLocationUseCase.getAddressFromPoint(point)
                .onRight {
                    onReverseGeocodedAddress.postValue(it)
                }
                .onLeft {
                    analytics.trackEventFailure(it.code)
                    onReverseGeocodedAddress.postValue(null)
                }
        }

        reverseGeocodingJob?.invokeOnCompletion {
            if (it is CancellationException) {
                Timber.d("Cancelled running reverse geocoding job.")
            }
        }
    }
}
