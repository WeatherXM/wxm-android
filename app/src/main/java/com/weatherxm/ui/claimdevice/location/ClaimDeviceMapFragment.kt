package com.weatherxm.ui.claimdevice.location

import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.weatherxm.R
import com.weatherxm.ui.BaseMapFragment
import com.weatherxm.ui.claimdevice.ClaimDeviceViewModel
import com.weatherxm.ui.claimdevice.location.ClaimDeviceLocationViewModel.Companion.ZOOM_LEVEL

class ClaimDeviceMapFragment : BaseMapFragment() {
    private val model: ClaimDeviceViewModel by activityViewModels()
    private val locationModel: ClaimDeviceLocationViewModel by activityViewModels()

    private lateinit var marker: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationModel.onLocationConfirmed().observe(viewLifecycleOwner) {
            if (it) {
                val location = viewManager.getViewAnnotationOptionsByView(marker)?.geometry as Point
                model.setInstallationLocation(location.latitude(), location.longitude())
            }
        }
    }

    override fun onMapReady(map: MapboxMap) {
        // No point executing if in the meanwhile the activity is dead
        if (context == null) {
            return
        }

        // Create default center marker
        marker = viewManager.addViewAnnotation(
            R.layout.view_marker,
            viewAnnotationOptions {
                anchor(ViewAnnotationAnchor.BOTTOM)
                geometry(map.cameraState.center)
                view?.measuredWidth?.let {
                    val padding = resources.getDimensionPixelSize(R.dimen.padding_normal)
                    width(it - 2 * padding)
                }
            }
        )

        // Add map camera change listener
        map.addOnCameraChangeListener {
            viewManager.updateViewAnnotation(marker, viewAnnotationOptions {
                geometry(map.cameraState.center)
            })
        }

        map.addOnMapIdleListener { locationModel.getAddressFromPoint(map.cameraState.center) }

        locationModel.onDeviceLocation().observe(this) {
            recenter(map, it)
        }

        locationModel.onSelectedSearchLocation().observe(this) {
            recenter(map, it)
        }

        locationModel.onReverseGeocodedAddress().observe(this) {
            val container = marker.findViewById<MaterialCardView>(R.id.addressContainer)
            if (it != null) {
                marker.findViewById<MaterialTextView>(R.id.address).text = it
                container.visibility = View.VISIBLE
            } else {
                container.visibility = View.INVISIBLE
            }
        }
    }

    // Update camera to a new center location
    private fun recenter(map: MapboxMap, location: Location) {
        map.setCamera(
            CameraOptions.Builder()
                .zoom(ZOOM_LEVEL)
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .build()
        )
    }
}
