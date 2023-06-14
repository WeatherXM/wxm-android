package com.weatherxm.ui.claimdevice.location

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.LocationHelper.getLocationAndThen
import com.weatherxm.util.Validator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions")
class ClaimLocationViewModel : ViewModel(), KoinComponent {
    // Current arbitrary values for the viewpager and the map
    companion object {
        const val ZOOM_LEVEL: Double = 15.0
        val REVERSE_GEOCODING_DELAY = TimeUnit.SECONDS.toMillis(1)
    }

    private val usecase: ClaimDeviceUseCase by inject()
    private val analytics: Analytics by inject()
    private val validator: Validator by inject()
    private var reverseGeocodingJob: Job? = null
    private var installationLocation = Location("").apply {
        latitude = 0.0
        longitude = 0.0
    }
    private var deviceType = DeviceType.M5_WIFI

    private val onRequestUserLocation = MutableLiveData<Boolean>(false)
    private val onDeviceLocation = MutableLiveData<Location?>()
    private val onSelectedSearchLocation = MutableLiveData<Location>()
    private val onLocationConfirmed = MutableLiveData(false)
    private val onSearchResults = MutableLiveData<List<SearchSuggestion>?>(mutableListOf())
    private val onReverseGeocodedAddress = MutableLiveData<String?>(null)

    fun onRequestUserLocation() = onRequestUserLocation
    fun onDeviceLocation() = onDeviceLocation
    fun onSearchResults() = onSearchResults
    fun onSelectedSearchLocation() = onSelectedSearchLocation
    fun onLocationConfirmed() = onLocationConfirmed
    fun onReverseGeocodedAddress() = onReverseGeocodedAddress

    fun requestUserLocation() {
        onRequestUserLocation.postValue(true)
    }

    fun confirmLocation() {
        onLocationConfirmed.postValue(true)
    }

    fun setDeviceType(type: DeviceType) {
        deviceType = type
    }

    fun getDeviceType(): DeviceType {
        return deviceType
    }

    fun validateLocation(lat: Double, lon: Double): Boolean {
        return validator.validateLocation(lat, lon)
    }

    fun setInstallationLocation(lat: Double, lon: Double) {
        installationLocation.latitude = lat
        installationLocation.longitude = lon
    }

    fun getInstallationLocation(): Location {
        return installationLocation
    }

    @RequiresPermission(anyOf = [ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION])
    fun getLocation(context: Context) {
        getLocationAndThen(context) {
            onDeviceLocation.postValue(it)
        }
    }

    fun geocoding(query: String) {
        viewModelScope.launch {
            usecase.getSearchSuggestions(query)
                .onRight { suggestions ->
                    onSearchResults.postValue(suggestions)
                }.onLeft {
                    onSearchResults.postValue(null)
                }
        }
    }

    fun getLocationFromSearchSuggestion(suggestion: SearchSuggestion) {
        viewModelScope.launch {
            usecase.getSuggestionLocation(suggestion)
                .onRight { location ->
                    onSelectedSearchLocation.postValue(location)
                }.onLeft {
                    analytics.trackEventFailure(it.code)
                }
        }
    }

    fun getAddressFromPoint(point: Point) {
        reverseGeocodingJob?.let {
            if (it.isActive) {
                it.cancel("Cancelling running reverse geocoding job.")
            }
        }

        reverseGeocodingJob = viewModelScope.launch {
            delay(REVERSE_GEOCODING_DELAY)
            usecase.getAddressFromPoint(point)
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
