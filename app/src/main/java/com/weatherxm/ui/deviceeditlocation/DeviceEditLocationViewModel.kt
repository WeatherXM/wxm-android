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
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.PublicHex
import com.weatherxm.ui.common.CapacityLayerOnSetLocation
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.components.BaseMapFragment.Companion.ON_MAP_IDLE_JOB_DELAY
import com.weatherxm.usecases.EditLocationUseCase
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.util.Failure.getDefaultMessageResId
import com.weatherxm.util.LocationHelper
import com.weatherxm.util.MapboxUtils.createCapacityLayer
import com.weatherxm.util.Resources
import com.weatherxm.util.Validator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class DeviceEditLocationViewModel(
    private val usecase: EditLocationUseCase,
    private val explorerUseCase: ExplorerUseCase,
    private val analytics: AnalyticsWrapper,
    private val locationHelper: LocationHelper,
    private val resources: Resources,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private var reverseGeocodingJob: Job? = null
    private var checkCellCapacityJob: Job? = null
    private var hexBounds: List<HexBounds> = listOf()
    private var isOnBelowCapacityHex: Boolean = true

    private data class HexBounds(
        val hex: PublicHex,
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    )

    private val onMoveToLocation = MutableLiveData<Location?>()
    private val onSearchResults = MutableLiveData<List<SearchSuggestion>?>(mutableListOf())
    private val onReverseGeocodedAddress = MutableLiveData<String?>(null)
    private val onUpdatedDevice = MutableLiveData<Resource<UIDevice>>()
    private val onCapacityLayer = MutableLiveData<CapacityLayerOnSetLocation?>()
    private val onCellWithBelowCapacity = MutableLiveData<Boolean?>(null)

    fun onMoveToLocation() = onMoveToLocation
    fun onSearchResults() = onSearchResults
    fun onReverseGeocodedAddress() = onReverseGeocodedAddress
    fun onUpdatedDevice(): LiveData<Resource<UIDevice>> = onUpdatedDevice
    fun onCapacityLayer() = onCapacityLayer
    fun onCellWithBelowCapacity() = onCellWithBelowCapacity

    fun validateLocation(lat: Double, lon: Double): Boolean {
        return Validator.validateLocation(lat, lon)
    }

    @RequiresPermission(anyOf = [ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION])
    fun getLocation() {
        locationHelper.getLocationAndThen {
            onMoveToLocation.postValue(it)
        }
    }

    fun getSearchSuggestions(query: String) {
        viewModelScope.launch(dispatcher) {
            usecase.getSearchSuggestions(query).onRight { suggestions ->
                onSearchResults.postValue(suggestions)
            }.onLeft {
                onSearchResults.postValue(null)
            }
        }
    }

    fun getLocationFromSearchSuggestion(suggestion: SearchSuggestion) {
        viewModelScope.launch(dispatcher) {
            usecase.getSuggestionLocation(suggestion).onRight { location ->
                onMoveToLocation.postValue(location)
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

        reverseGeocodingJob = viewModelScope.launch(dispatcher) {
            delay(ON_MAP_IDLE_JOB_DELAY)
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

    fun setLocation(deviceId: String, lat: Double, lon: Double) {
        viewModelScope.launch(dispatcher) {
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

    fun getCells() {
        viewModelScope.launch(dispatcher) {
            explorerUseCase.getCells().onRight { response ->
                hexBounds = response.publicHexes.map { hex ->
                    HexBounds(
                        hex = hex,
                        minLat = hex.polygon.minOf { it.lat },
                        maxLat = hex.polygon.maxOf { it.lat },
                        minLon = hex.polygon.minOf { it.lon },
                        maxLon = hex.polygon.maxOf { it.lon }
                    )
                }
                onCapacityLayer.postValue(createCapacityLayer(response.publicHexes))
            }
        }
    }

    fun isPointOnBelowCapacityCell(lat: Double, lon: Double) {
        onCellWithBelowCapacity.postValue(null)
        checkCellCapacityJob?.let {
            if (it.isActive) {
                it.cancel("Cancelling running job of checking cell capacity of point.")
            }
        }

        checkCellCapacityJob = viewModelScope.launch(dispatcher) {
            delay(ON_MAP_IDLE_JOB_DELAY)

            val potentialHex = hexBounds.firstOrNull { bounds ->
                // Quick bounds check eliminates ~95% of hexes
                if (lat < bounds.minLat || lat > bounds.maxLat ||
                    lon < bounds.minLon || lon > bounds.maxLon
                ) {
                    false
                } else {
                    // Only do expensive polygon check for candidates
                    bounds.hex.isPointInConvexPolygon(lat, lon)
                }
            }?.hex

            onCellWithBelowCapacity.postValue(potentialHex == null || potentialHex.isBelowCapacity())
        }

        checkCellCapacityJob?.invokeOnCompletion {
            if (it is CancellationException) {
                Timber.d("Cancelled running job of checking cell capacity of point..")
            }
        }
    }
}
