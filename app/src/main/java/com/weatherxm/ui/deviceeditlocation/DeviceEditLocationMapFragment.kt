package com.weatherxm.ui.deviceeditlocation

import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.weatherxm.R
import com.weatherxm.data.Location
import com.weatherxm.ui.BaseMapFragment
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class DeviceEditLocationMapFragment : BaseMapFragment() {
    private val model: DeviceEditLocationViewModel by activityViewModels()
    private var marker: View? = null

    override fun onMapReady(map: MapboxMap) {
        // No point executing if in the meanwhile the activity is dead
        if (context == null) {
            return
        }

        binding.appBar.setVisible(false)
        binding.searchView.setVisible(false)

        model.onGoToLocation().observe(this) {
            Timber.d("Got new location: $it")
            if (it == null) {
                context.toast(R.string.error_claim_gps_failed)
            } else {
                recenter(map, it)
            }
        }

        model.onSelectedSearchLocation().observe(this) {
            recenter(map, it)
        }
    }

    /**
     * For some reason because of the fact that we start with the map being in GONE state, the
     * marker doesn't work properly. Though this function is needed to be run AFTER the map becomes
     * visible.
     *
     * The `delay` is also necessary for it to work.
     */
    @Suppress("MagicNumber")
    fun initMarkerAndListeners() {
        lifecycleScope.launch {
            delay(100L)
            val map = getMap()

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

            marker?.let {
                // Add map camera change listener
                map.addOnCameraChangeListener { _ ->
                    viewManager.updateViewAnnotation(it, viewAnnotationOptions {
                        geometry(map.cameraState.center)
                    })
                }
            }

            map.addOnMapIdleListener { model.getAddressFromPoint(map.cameraState.center) }

            model.onReverseGeocodedAddress().observe(
                this@DeviceEditLocationMapFragment
            ) { address ->
                marker?.let {
                    val container = it.findViewById<MaterialCardView>(R.id.addressContainer)
                    if (address != null) {
                        it.findViewById<MaterialTextView>(R.id.address).text = address
                        container.visibility = View.VISIBLE
                    } else {
                        container.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    fun onConfirmClicked(deviceId: String) {
        marker?.let {
            val loc = viewManager.getViewAnnotationOptionsByView(it)?.geometry as Point

            if (!model.validateLocation(loc.latitude(), loc.longitude())) {
                activity?.toast(R.string.invalid_location)
                return
            }
            model.confirmLocation(deviceId, loc.latitude(), loc.longitude())
        }
    }

    // Update camera to a new center location
    private fun recenter(map: MapboxMap, location: Location) {
        map.setCamera(
            CameraOptions.Builder()
                .zoom(USER_LOCATION_ZOOM_LEVEL)
                .center(Point.fromLngLat(location.lon, location.lat))
                .build()
        )
    }
}
