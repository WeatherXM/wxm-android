package com.weatherxm.ui.explorer

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.weatherxm.data.Status
import com.weatherxm.ui.BaseMapFragment
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.ZOOM_H3_CLICK
import timber.log.Timber

class ExplorerMapFragment : BaseMapFragment() {
    /*
        Use activityViewModels because we use this model to communicate with the parent activity
        so it needs to be the same model as the parent's one.
    */
    private val model: ExplorerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mapStyle = "mapbox://styles/exmachina/ckrxjh01a5e7317plznjeicao"
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onMapReady(map: MapboxMap) {
        polygonManager.addClickListener {
            model.onPolygonClick(it)
            true
        }

        map.addOnMapClickListener {
            model.onMapClick()
            true
        }

        model.explorerState().observe(this) { resource ->
            Timber.d("Data updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
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

        model.onZoomChange().observe(this) { newZoom ->
            val newPointCameraOptions = CameraOptions.Builder()
                .center(newZoom)
                .zoom(ZOOM_H3_CLICK)
                .build()

            map.easeTo(newPointCameraOptions, null)
        }

        binding.mapView.camera.addCameraZoomChangeListener { zoom ->
            Timber.d("New zoom: $zoom")
            model.changeHexSize(zoom)
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
}
