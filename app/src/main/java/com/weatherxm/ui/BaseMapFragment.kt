package com.weatherxm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.scalebar.scalebar
import com.weatherxm.databinding.FragmentMapBinding
import dev.chrisbanes.insetter.applyInsetter
import org.koin.core.component.KoinComponent
import timber.log.Timber

open class BaseMapFragment : Fragment(), KoinComponent {

    protected lateinit var binding: FragmentMapBinding
    protected lateinit var polygonManager: PolygonAnnotationManager
    protected lateinit var pointManager: PointAnnotationManager

    protected var mapStyle: String = Style.MAPBOX_STREETS
    private var defaultStartingZoomLevel: Double = 1.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)

        binding.progress.applyInsetter {
            type(statusBars = true) {
                margin(left = false, top = true, right = false, bottom = false)
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val map = binding.mapView.getMapboxMap()
        map.loadStyleUri(mapStyle) {
            Timber.d("MapBox is ready and style loaded")

            binding.mapView.gestures.rotateEnabled = false
            binding.mapView.scalebar.enabled = false

            with(binding.mapView.annotations) {
                polygonManager = this.createPolygonAnnotationManager()
                pointManager = this.createPointAnnotationManager()
            }

            val cameraPosition = CameraOptions.Builder()
                .zoom(defaultStartingZoomLevel)
                .build()

            // Update camera
            map.setCamera(cameraPosition)

            onMapReady(map)
        }
    }

    fun getMap(): MapboxMap = binding.mapView.getMapboxMap()

    open fun onMapReady(map: MapboxMap) {
        // Override in subclasses
    }
}
