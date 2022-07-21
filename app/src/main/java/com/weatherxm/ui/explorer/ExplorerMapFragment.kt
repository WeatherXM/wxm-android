package com.weatherxm.ui.explorer

import android.view.View
import androidx.fragment.app.activityViewModels
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.weatherxm.data.Status
import com.weatherxm.ui.BaseMapFragment
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.HEATMAP_SOURCE_ID
import timber.log.Timber

class ExplorerMapFragment : BaseMapFragment() {
    /*
        Use activityViewModels because we use this model to communicate with the parent activity
        so it needs to be the same model as the parent's one.
    */
    private val model: ExplorerViewModel by activityViewModels()

    override fun onMapReady(map: MapboxMap) {
        polygonManager.addClickListener {
            model.onPolygonClick(it)
            true
        }

        map.addOnMapClickListener {
            model.onMapClick()
            true
        }

        map.addOnCameraChangeListener {
            model.setCurrentCamera(map.cameraState.zoom, map.cameraState.center)
        }

        model.explorerState().observe(this) { resource ->
            Timber.d("Data updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    resource.data?.geoJsonSource?.let { source ->
                        val mapStyle = map.getStyle()
                        if (mapStyle?.styleSourceExists(HEATMAP_SOURCE_ID) == true) {
                            source.data?.let {
                                (mapStyle.getSource(HEATMAP_SOURCE_ID) as GeoJsonSource).data(it)
                            }
                        } else {
                            mapStyle?.addSource(source)
                            mapStyle?.addLayerAbove(model.getHeatMapLayer(), "waterway-label")
                        }
                    }
                    onPolygonPointsUpdated(resource.data?.polygonPoints)
                    binding.progress.visibility = View.INVISIBLE
                }
                Status.ERROR -> {
                    binding.progress.visibility = View.INVISIBLE
                }
                Status.LOADING -> {
                    binding.progress.visibility = View.VISIBLE
                }
            }
        }

        // Set camera to the last saved location the user was at
        model.getCurrentCamera()?.let {
            map.setCamera(
                CameraOptions.Builder()
                    .center(it.center)
                    .zoom(it.zoom)
                    .build()
            )
        }

        // Fetch data
        model.fetch()
    }

    private fun onPolygonPointsUpdated(polygonPoints: List<PolygonAnnotationOptions>?) {
        if (polygonPoints.isNullOrEmpty()) {
            Timber.d("No devices found. Skipping map update.")
            return
        }

        // First clear the map and the relevant attributes
        polygonManager.deleteAll()
        polygonManager.create(polygonPoints)
    }

    override fun getMapStyle(): String {
        return "mapbox://styles/exmachina/ckrxjh01a5e7317plznjeicao"
    }
}
