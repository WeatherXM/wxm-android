package com.weatherxm.ui.explorer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotation
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.util.MapboxUtils
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ExplorerViewModel : ViewModel(), KoinComponent {
    // Current arbitrary zoom level to change from H3->H7 and vice versa
    companion object {
        const val ZOOM_LEVEL_CHANGE_HEX: Double = 6.0
        const val ZOOM_H3_CLICK: Double = 7.0
        const val H3_RESOLUTION: Int = 3
        const val H7_RESOLUTION: Int = 7
        const val FILL_OPACITY_HEXAGONS: Double = 0.5
    }

    private val explorerUseCase: ExplorerUseCase by inject()

    private var currentZoom: Double = 0.0
    private var currentExplorerState = ExplorerState(null)

    // All public devices shown on map
    private val state = MutableLiveData<Resource<ExplorerState>>().apply {
        value = Resource.loading()
    }

    // The list of a devices in a hex
    private val onHexSelected = MutableLiveData<String>()

    // The details/data of a device
    private val onDeviceSelected = MutableLiveData<Device>()

    private val onZoomChange = MutableLiveData<Point?>()

    // Needed for passing info to the activity to show/hide elements when onMapClick
    private val showMapOverlayViews = MutableLiveData(true)

    // Current Hex clicked as it's needed to pass it as an argument in the device details
    private var currentHexSelected: String? = null

    // To observe the state of the fetching process, in order to not make multiple same calls
    private var fetchingJob: Job? = null

    fun showMapOverlayViews() = showMapOverlayViews
    fun explorerState(): LiveData<Resource<ExplorerState>> = state
    fun onHexSelected(): LiveData<String> = onHexSelected
    fun onDeviceSelected(): LiveData<Device> = onDeviceSelected
    fun onZoomChange(): LiveData<Point?> = onZoomChange

    fun fetch(forceRefresh: Boolean = false) {
        if (fetchingJob == null || fetchingJob?.isActive == false) {
            state.postValue(Resource.loading())

            fetchingJob = viewModelScope.launch {
                explorerUseCase.getPublicDevices(forceRefresh)
                    .map {
                        val polygonPoints = explorerUseCase.getPointsFromPublicDevices(currentZoom)
                        Timber.d("Got Polygon Points: $polygonPoints")
                        currentExplorerState.polygonPoints = polygonPoints
                        state.postValue(Resource.success(currentExplorerState))
                    }
                    .mapLeft {
                        state.postValue(Resource.error(it.getDefaultMessage()))
                    }
            }
        }
    }

    fun changeHexSize(newZoom: Double) {
        if (currentZoom < ZOOM_LEVEL_CHANGE_HEX && newZoom >= ZOOM_LEVEL_CHANGE_HEX) {
            currentZoom = newZoom
            fetch()
        } else if (currentZoom >= ZOOM_LEVEL_CHANGE_HEX && newZoom < ZOOM_LEVEL_CHANGE_HEX) {
            currentZoom = newZoom
            fetch()
        }
    }

    fun onMapClick() {
        val current = showMapOverlayViews.value == true
        showMapOverlayViews.postValue(!current)
    }

    /**
     * Handler for polygon clicks. If camera level is at H7, then show devices list for that hex
     * otherwise zoom in.
     */
    fun onPolygonClick(polygon: PolygonAnnotation) {
        val hexWithResolution: HexWithResolution? = MapboxUtils.getCustomData(polygon)
        if (hexWithResolution?.currentResolution == H7_RESOLUTION) {
            currentHexSelected = hexWithResolution.index
            onHexSelected.postValue(hexWithResolution.index)
        } else {
            val centerOfHex = explorerUseCase.getCenterOfHex3AsPoint(hexWithResolution)
            centerOfHex?.let { onZoomChange.postValue(centerOfHex) }
        }
    }

    fun onDeviceClicked(device: Device) {
        onDeviceSelected.postValue(device)
    }

    fun openListOfDevicesOfHex() {
        currentHexSelected?.let {
            onHexSelected.postValue(it)
        }
    }
}
