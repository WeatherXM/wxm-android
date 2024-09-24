package com.weatherxm.ui.deviceeditlocation

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.R
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.Location
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.components.BaseMapFragment.Companion.REVERSE_GEOCODING_DELAY
import com.weatherxm.usecases.EditLocationUseCase
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.util.Failure.getDefaultMessageResId
import com.weatherxm.util.LocationHelper
import com.weatherxm.util.Resources
import com.weatherxm.util.Validator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("TooManyFunctions")
class DeviceEditLocationViewModel(
    private val usecase: EditLocationUseCase,
    private val analytics: AnalyticsWrapper,
    private val locationHelper: LocationHelper,
    private val resources: Resources
) : ViewModel() {
    private var reverseGeocodingJob: Job? = null
    private var installationLocation = Location(0.0, 0.0)

    private val onMoveToLocation = MutableLiveData<Location?>()
    private val onSearchResults = MutableLiveData<List<SearchSuggestion>?>(mutableListOf())
    private val onReverseGeocodedAddress = MutableLiveData<String?>(null)
    private val onUpdatedDevice = MutableLiveData<Resource<UIDevice>>()

    fun onMoveToLocation() = onMoveToLocation
    fun onSearchResults() = onSearchResults
    fun onReverseGeocodedAddress() = onReverseGeocodedAddress
    fun onUpdatedDevice(): LiveData<Resource<UIDevice>> = onUpdatedDevice

    fun validateLocation(lat: Double, lon: Double): Boolean {
        return Validator.validateLocation(lat, lon)
    }

    @RequiresPermission(anyOf = [ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION])
    fun getLocation() {
        locationHelper.getLocationAndThen {
            onMoveToLocation.postValue(it)
        }
    }

    fun geocoding(query: String) {
        viewModelScope.launch {
            usecase.getSearchSuggestions(query).onRight { suggestions ->
                onSearchResults.postValue(suggestions)
            }.onLeft {
                onSearchResults.postValue(null)
            }
        }
    }

    fun getLocationFromSearchSuggestion(suggestion: SearchSuggestion) {
        viewModelScope.launch {
            usecase.getSuggestionLocation(suggestion).onRight { location ->
                onMoveToLocation.postValue(location)
            }.onLeft {
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
            usecase.getAddressFromPoint(point).onRight {
                onReverseGeocodedAddress.postValue(it)
            }.onLeft {
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

    fun confirmLocation(deviceId: String, lat: Double, lon: Double) {
        installationLocation.lat = lat
        installationLocation.lon = lon

        viewModelScope.launch {
            onUpdatedDevice.postValue(Resource.loading())
            usecase.setLocation(deviceId, lat, lon).onRight {
                Timber.d("Location Updated. Got new device.")
                onUpdatedDevice.postValue(Resource.success(it))
            }.onLeft {
                analytics.trackEventFailure(it.code)
                Timber.e("Error on location $it")
                val message = resources.getString(
                    when (it) {
                        is ApiError.UserError.ClaimError.InvalidClaimLocation -> {
                            R.string.error_invalid_location
                        }
                        is ApiError.DeviceNotFound -> R.string.error_device_not_found
                        else -> it.getDefaultMessageResId()
                    }
                )
                onUpdatedDevice.postValue(Resource.error(message))
            }
        }
    }
}
