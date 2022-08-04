package com.weatherxm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.geojson.Point
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
import com.weatherxm.ui.BaseMapFragment.OnMapDebugInfoListener
import dev.chrisbanes.insetter.applyInsetter
import timber.log.Timber

open class BaseMapFragment : Fragment() {

    fun interface OnMapDebugInfoListener {
        fun onMapDebugInfoUpdated(zoom: Double, center: Point)
    }

    protected lateinit var binding: FragmentMapBinding
    protected lateinit var polygonManager: PolygonAnnotationManager
    protected lateinit var pointManager: PointAnnotationManager

    private lateinit var debugInfoListener: OnMapDebugInfoListener
    private var defaultStartingZoomLevel: Double = 1.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        debugInfoListener = try {
            activity as OnMapDebugInfoListener
        } catch (e: ClassCastException) {
            Timber.d(e, "Parent activity does not implement OnMapDebugInfoListener.")
            OnMapDebugInfoListener { _, _ ->  /* NOOP */ }
        }
    }

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

        map.addOnCameraChangeListener {
            debugInfoListener.onMapDebugInfoUpdated(
                zoom = map.cameraState.zoom,
                center = map.cameraState.center
            )
        }

        map.loadStyleUri(getMapStyle()) {
            Timber.d("MapBox is ready and style loaded")

            binding.mapView.gestures.rotateEnabled = false
            binding.mapView.scalebar.enabled = false

            with(binding.mapView.annotations) {
                polygonManager = this.createPolygonAnnotationManager()
                pointManager = this.createPointAnnotationManager()
            }

            // Update camera
            map.setCamera(
                CameraOptions.Builder()
                    .zoom(defaultStartingZoomLevel)
                    .build()
            )

            onMapReady(map)
        }
    }

    fun getMap(): MapboxMap = binding.mapView.getMapboxMap()

    open fun onMapReady(map: MapboxMap) {
        // Override in subclasses
    }

    open fun getMapStyle(): String {
        return Style.MAPBOX_STREETS
    }
}
