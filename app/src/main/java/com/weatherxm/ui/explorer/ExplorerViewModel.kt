package com.weatherxm.ui.explorer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotation
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.Resource
import com.weatherxm.data.ServerError
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.util.MapboxUtils
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val resourcesHelper: ResourcesHelper by inject()

    private var currentZoom: Double = 0.0
    private var currentExplorerState = ExplorerState(null, null, null)

    // All public devices shown on map
    private val state = MutableLiveData<Resource<ExplorerState>>().apply {
        value = Resource.loading()
    }

    // The details/data of a device
    private val onDeviceSelected = MutableLiveData<PublicDevice>()

    private val onZoomChange = MutableLiveData<Point?>()

    // Needed for passing info to the activity to show/hide elements when onMapClick
    private val showMapOverlayViews = MutableLiveData(true)

    fun showMapOverlayViews() = showMapOverlayViews
    fun explorerState(): LiveData<Resource<ExplorerState>> = state
    fun onDeviceSelected(): LiveData<PublicDevice> = onDeviceSelected
    fun onZoomChange(): LiveData<Point?> = onZoomChange

    fun fetch() {
        state.postValue(Resource.loading(currentExplorerState))

        CoroutineScope(Dispatchers.IO).launch {
            explorerUseCase.getPointsFromPublicDevices(currentZoom)
                .map { polygonPoints ->
                    Timber.d("Got Polygon Points: $polygonPoints")
                    currentExplorerState.polygonPoints = polygonPoints
                    state.postValue(
                        Resource.success(currentExplorerState)
                    )
                }
                .mapLeft {
                    Timber.d("Got error: $it")
                    when (it) {
                        is Failure.NetworkError -> state.postValue(
                            Resource.error(
                                resourcesHelper.getString(R.string.network_error)
                            )
                        )
                        is ServerError -> state.postValue(
                            Resource.error(
                                resourcesHelper.getString(R.string.server_error)
                            )
                        )
                        is Failure.UnknownError -> state.postValue(
                            Resource.error(
                                resourcesHelper.getString(R.string.unknown_error_public_devices)
                            )
                        )
                        else -> state.postValue(
                            Resource.error(
                                resourcesHelper.getString(R.string.unknown_error_public_devices)
                            )
                        )
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

    private fun onDeviceClick(publicDevice: PublicDevice) {
        onDeviceSelected.postValue(publicDevice)
    }

    /**
     * Handler for polygon clicks. If camera level is at H7, then show weather
     * station details  (weather data card, etc.), otherwise zoom in.
     */
    fun onPolygonClick(polygon: PolygonAnnotation) {
        val deviceWithRes: DeviceWithResolution? = MapboxUtils.getCustomData(polygon)
        if (deviceWithRes?.currentResolution == H7_RESOLUTION) {
            onDeviceClick(deviceWithRes.device)
        } else {
            val centerOfHex = explorerUseCase.getCenterOfHex3(deviceWithRes)
            centerOfHex?.let { onZoomChange.postValue(centerOfHex) }
        }
    }
}
