package com.weatherxm.ui.components

import android.view.View
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.weatherxm.R
import com.weatherxm.data.models.Location
import com.weatherxm.ui.common.CapacityLayerOnSetLocation
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import timber.log.Timber

class EditLocationMapFragment : BaseMapFragment() {
    private var marker: View? = null
    private var map: MapboxMap? = null
    private var listener: EditLocationListener? = null

    override fun onMapReady(map: MapboxMap) {
        // No point executing if in the meanwhile the activity is dead
        if (context == null) {
            return
        }
        this.map = map

        binding.topBar.visible(false)
        binding.searchView.visible(false)
        this.listener?.onMapReady()
    }

    fun setListener(listener: EditLocationListener) {
        this.listener = listener
    }

    fun moveToLocation(location: Location?) {
        Timber.d("Got new location: $location")
        if (location == null) {
            context.toast(R.string.error_claim_gps_failed)
        } else {
            recenter(map, location)
        }
    }

    fun initMarkerAndListeners() {
        val map = getMap()

        marker = viewManager.addViewAnnotation(
            R.layout.view_marker,
            viewAnnotationOptions {
                annotationAnchor {
                    anchor(ViewAnnotationAnchor.BOTTOM)
                }
                geometry(map.cameraState.center)
                view?.measuredWidth?.let {
                    val padding = resources.getDimensionPixelSize(R.dimen.padding_normal)
                    width(it - 2.0 * padding)
                }
            }
        )

        marker?.let {
            // Add map camera change listener
            map.subscribeCameraChanged { _ ->
                viewManager.updateViewAnnotation(it, viewAnnotationOptions {
                    geometry(map.cameraState.center)
                })
            }
            it.findViewById<MaterialCardView>(R.id.addressContainer).invisible()
        }
    }

    fun addOnMapIdleListener(onAddressFromPoint: (Point?) -> Unit) {
        map?.subscribeMapIdle { onAddressFromPoint(map?.cameraState?.center) }
    }

    fun showMarkerAddress(address: String?) {
        marker?.let {
            val container = it.findViewById<MaterialCardView>(R.id.addressContainer)
            if (address != null) {
                it.findViewById<MaterialTextView>(R.id.address).text = address
                container.visible(true)
            } else {
                container.invisible()
            }
        }
    }

    fun getMarkerLocation(): Location {
        return marker?.let {
            val point =
                viewManager.getViewAnnotationOptions(it)?.annotatedFeature?.geometry as Point
            Location(point.latitude(), point.longitude())
        } ?: Location.empty()
    }

    // Update camera to a new center location
    private fun recenter(map: MapboxMap?, location: Location) {
        map?.setCamera(
            CameraOptions.Builder()
                .zoom(USER_SET_LOCATION_ZOOM_LEVEL)
                .center(Point.fromLngLat(location.lon, location.lat))
                .build()
        )
    }

    fun drawCapacityLayers(layerData: CapacityLayerOnSetLocation) {
        getMap().style?.apply {
            // Add source if it doesn't exist
            if (!styleSourceExists("capacity-source")) {
                addSource(layerData.source)
            }

            // Add fill layer
            if (!styleLayerExists("capacity-fill-layer")) {
                addLayerAbove(layerData.fillLayer, "waterway-label")
            }

            // Add line layer above fill layer
            if (!styleLayerExists("capacity-outline-layer")) {
                addLayerAbove(layerData.lineLayer, "capacity-fill-layer")
            }

            // Add text layer above line layer
            if (!styleLayerExists("capacity-text-layer")) {
                addLayerAbove(layerData.textLayer, "capacity-outline-layer")
            }
        }
    }
}

interface EditLocationListener {
    fun onMapReady()
}
