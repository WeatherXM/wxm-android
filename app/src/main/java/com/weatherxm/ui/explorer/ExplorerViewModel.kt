package com.weatherxm.ui.explorer

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.layers.generated.HeatmapLayer
import com.mapbox.maps.extension.style.layers.generated.heatmapLayer
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotation
import com.weatherxm.R
import com.weatherxm.data.Location
import com.weatherxm.data.Resource
import com.weatherxm.data.SingleLiveEvent
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.LocationHelper
import com.weatherxm.util.MapboxUtils
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

@Suppress("TooManyFunctions")
class ExplorerViewModel : ViewModel(), KoinComponent {
    companion object {
        const val FILL_OPACITY_HEXAGONS: Double = 0.5
        const val HEATMAP_SOURCE_ID = "heatmap"
        const val HEATMAP_LAYER_ID = "heatmap-layer"
        const val HEATMAP_LAYER_SOURCE = "heatmap"
        const val HEATMAP_WEIGHT_KEY = ExplorerUseCase.DEVICE_COUNT_KEY
        const val USER_LOCATION_DEFAULT_ZOOM_LEVEL: Double = 11.0
    }

    private val explorerUseCase: ExplorerUseCase by inject()
    private val analytics: Analytics by inject()
    private val locationHelper: LocationHelper by inject()

    // All public devices shown on map
    private val state = MutableLiveData<Resource<ExplorerData>>().apply {
        value = Resource.loading()
    }

    /*
     * The cell info.
     *
     * We use SingleLiveEvent because MutableLiveData persists and re-posts the value
     * to the observers on configuration change (like a theme change) and the effects of the
     * observers happen again (like re-opening a closed BottomSheetDialog).
     */
    private val onCellSelected = SingleLiveEvent<UICell>()

    /**
     * Needed for passing info to the fragment to handle when a prefilled location is used
     * so we want the camera to move directly to that point
     */
    private val onNavigateToLocation = SingleLiveEvent<Location>()

    /**
     * Needed for passing info to the fragment to handle when a starting location is used
     * so we want the camera to move directly to that point (usually country level but on low zoom)
     */
    private val onStartingLocation = SingleLiveEvent<Location>()

    // Needed for passing info to the fragment to handle when my location button is clicked
    private val onMyLocationClicked = SingleLiveEvent<Boolean>()

    // Needed for passing info to the activity to show/hide elements when onMapClick
    private val showMapOverlayViews = MutableLiveData(true)

    // Needed for passing info to the activity to show/hide elements when search view is opened
    private val onSearchOpenStatus = MutableLiveData(false)

    // Save the current explorer camera zoom and center
    private var currentCamera: ExplorerCamera? = null

    // Save the current status of where the explorer is located (before/after login)
    private var explorerAfterLoggedIn = false

    @Suppress("MagicNumber")
    private val heatmapLayer: HeatmapLayer by lazy {
        heatmapLayer(
            HEATMAP_LAYER_ID,
            HEATMAP_SOURCE_ID
        ) {
            maxZoom(10.0) // Hide layer at zoom level 10
            sourceLayer(HEATMAP_LAYER_SOURCE)
            // Begin color ramp at 0-stop with a 0-transparency color
            // to create a blur-like effect.
            heatmapColor(
                interpolate {
                    linear()
                    heatmapDensity()
                    stop {
                        literal(0)
                        rgba(33.0, 102.0, 172.0, 0.0)
                    }
                    stop {
                        literal(0.2)
                        rgb(103.0, 169.0, 207.0)
                    }
                    stop {
                        literal(0.4)
                        rgb(162.0, 187.0, 201.0)
                    }
                    stop {
                        literal(0.6)
                        rgb(149.0, 153.0, 189.0)
                    }
                    stop {
                        literal(0.8)
                        rgb(103.0, 118.0, 247.0)
                    }
                    stop {
                        literal(1)
                        rgb(0.0, 255.0, 206.0)
                    }
                }
            )
            // Increase the heatmap weight based on the device count
            heatmapWeight(
                interpolate {
                    linear()
                    get { literal(HEATMAP_WEIGHT_KEY) }
                    stop {
                        literal(0)
                        literal(0) // 0 weight at 0 cell device count
                    }
                    stop {
                        literal(100)
                        literal(100) // 100 weight at 100 cell device count
                    }
                }
            )
            // Adjust the heatmap radius by zoom level
            heatmapRadius(
                interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(2) // 2 radius at zoom level 0.0
                    }
                    stop {
                        literal(9.0)
                        literal(20) // 20 radius at zoom level 9.0
                    }
                }
            )
            // Adjust heatmap opacity based on zoom level, until it totally disappears
            heatmapOpacity(
                interpolate {
                    exponential {
                        literal(0.5) // Exponential interpolation base
                    }
                    zoom()
                    stop {
                        literal(0.0)
                        literal(1.0) // 1.0 opacity at zoom level 0.0
                    }
                    stop {
                        literal(8.0)
                        literal(0.9) // 0.9 opacity at zoom level 8.0
                    }
                    stop {
                        literal(9.0)
                        literal(0.5) // 0.5 opacity at zoom level 9.0
                    }
                    stop {
                        literal(9.5)
                        literal(0.1) // 0.1 opacity at zoom level 9.5
                    }
                    stop {
                        literal(10.0)
                        literal(0.0)  // 0.0 opacity at zoom level 10.0
                    }
                }
            )
        }
    }

    fun onMyLocationClicked() = onMyLocationClicked
    fun onNavigateToLocation() = onNavigateToLocation
    fun onStartingLocation() = onStartingLocation
    fun showMapOverlayViews() = showMapOverlayViews
    fun onSearchOpenStatus() = onSearchOpenStatus
    fun explorerState(): LiveData<Resource<ExplorerData>> = state
    fun onCellSelected(): LiveData<UICell> = onCellSelected

    fun navigateToLocation(location: Location) {
        onNavigateToLocation.postValue(location)
    }

    fun setExplorerAfterLoggedIn(isAfterLoggedIn: Boolean) {
        explorerAfterLoggedIn = isAfterLoggedIn
    }

    fun isExplorerAfterLoggedIn(): Boolean {
        return explorerAfterLoggedIn
    }

    fun onMyLocation() {
        onMyLocationClicked.postValue(true)
    }

    fun fetch() {
        state.postValue(Resource.loading())

        viewModelScope.launch {
            explorerUseCase.getCells()
                .map {
                    state.postValue(Resource.success(it))
                }
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    state.postValue(
                        Resource.error(it.getDefaultMessage(R.string.error_reach_out_short))
                    )
                }
        }
    }

    fun onMapClick() {
        val current = showMapOverlayViews.value == true
        showMapOverlayViews.postValue(!current)
    }

    fun onSearchOpenStatus(isOpen: Boolean) {
        onSearchOpenStatus.postValue(isOpen)
    }

    /**
     * Handler for polygon clicks. Show devices list for that hex.
     */
    fun onPolygonClick(polygon: PolygonAnnotation) {
        MapboxUtils.getCustomData(polygon)?.let {
            onCellSelected.postValue(it)
        }
    }

    fun setCurrentCamera(zoom: Double, center: Point) {
        currentCamera = ExplorerCamera(zoom, center)
    }

    fun getCurrentCamera(): ExplorerCamera? {
        return currentCamera
    }

    @JvmName("getHeatMapLayer")
    fun getHeatMapLayer(): HeatmapLayer = heatmapLayer

    @SuppressLint("MissingPermission")
    fun getLocation(onLocation: (location: Location?) -> Unit) {
        locationHelper.getLocationAndThen(onLocation)
    }

    init {
        if (locationHelper.hasLocationPermissions()) {
            getLocation {
                it?.let {
                    onStartingLocation.postValue(it)
                }
            }
        } else {
            explorerUseCase.getUserCountryLocation()?.let {
                Timber.d("Got starting location [${it.lat}, ${it.lon}")
                onStartingLocation.postValue(it)
            }
        }
    }
}
