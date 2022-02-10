package com.weatherxm.ui.claimdevice

import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.activityViewModels
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.weatherxm.R
import com.weatherxm.ui.BaseMapFragment
import com.weatherxm.ui.claimdevice.ClaimDeviceViewModel.Companion.ZOOM_LEVEL

class ClaimDeviceMapFragment : BaseMapFragment() {

    private val model: ClaimDeviceViewModel by activityViewModels()

    private lateinit var marker: PointAnnotation
    private lateinit var listener: OnCameraChangeListener

    override fun onMapReady(map: MapboxMap) {
        if (context == null) {
            // No point executing if in the meanwhile the activity is dead
            return
        }

        // Create default center marker
        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker)?.toBitmap()
            ?: BitmapFactory.decodeResource(resources, R.drawable.ic_marker_default)
        marker = pointManager.create(
            PointAnnotationOptions()
                .withIconImage(icon)
                .withPoint(map.cameraState.center)
        )

        // Add map camera change listener
        listener = OnCameraChangeListener {
            with(marker) {
                point = map.cameraState.center
                pointManager.update(this)
            }
        }
        map.addOnCameraChangeListener(listener)

        model.onDeviceLocation().observe(this) { location ->
            val point = Point.fromLngLat(location.longitude, location.latitude)
            val cameraPosition = CameraOptions.Builder()
                .zoom(ZOOM_LEVEL)
                .center(point)
                .build()

            // Update camera
            map.setCamera(cameraPosition)

            // Move point
            marker.point = point
            pointManager.update(marker)
        }

        model.onLocationSet().observe(this) { shouldSetLocation ->
            if (shouldSetLocation) {
                model.setLocation(marker.point.longitude(), marker.point.latitude())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.useGps()
    }

    override fun onDestroy() {
        getMap().removeOnCameraChangeListener(listener)
        super.onDestroy()
    }
}
