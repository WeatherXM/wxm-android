package com.weatherxm.ui.explorer

import android.Manifest
import android.annotation.SuppressLint
import android.view.View
import androidx.fragment.app.activityViewModels
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.ui.BaseMapFragment
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Animation
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.common.hide
import com.weatherxm.ui.common.show
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.HEATMAP_SOURCE_ID
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.USER_LOCATION_DEFAULT_ZOOM_LEVEL
import dev.chrisbanes.insetter.applyInsetter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ExplorerMapFragment : BaseMapFragment(), KoinComponent {
    /*
        Use activityViewModels because we use this model to communicate with the parent activity
        so it needs to be the same model as the parent's one.
    */
    private val navigator: Navigator by inject()
    private val model: ExplorerViewModel by activityViewModels()

    override fun onMapReady(map: MapboxMap) {
        binding.appBar.applyInsetter {
            type(statusBars = true) {
                margin(left = false, top = true, right = false, bottom = false)
            }
        }

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

        getMapView().let {
            it.location.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
        }

        model.showMapOverlayViews().observe(this) {
            onShowMapOverlayViews(it)
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.settings -> {
                    navigator.showPreferences(this)
                    true
                }
                else -> false
            }
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

        binding.myLocationButton.setOnClickListener {
            requestLocationPermissions()
        }

        // Fetch data
        model.fetch()
    }

    private fun onShowMapOverlayViews(shouldShow: Boolean) {
        if (model.isExplorerAfterLoggedIn()) {
            if (shouldShow) {
                binding.myLocationButton.show(Animation.ShowAnimation.SlideInFromTop)
            } else {
                binding.myLocationButton.hide(Animation.HideAnimation.SlideOutToTop)
            }
            binding.appBar.visibility = View.GONE
        } else {
            if (shouldShow) {
                binding.appBar.show(Animation.ShowAnimation.SlideInFromTop)
                binding.myLocationButton.show(Animation.ShowAnimation.SlideInFromTop)
            } else {
                binding.appBar.hide(Animation.HideAnimation.SlideOutToTop)
                binding.myLocationButton.hide(Animation.HideAnimation.SlideOutToTop)
            }
        }
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

    @SuppressLint("MissingPermission")
    private fun requestLocationPermissions() {
        context?.let { context ->
            checkPermissionsAndThen(
                permissions = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                rationaleTitle = getString(R.string.permission_location_title),
                rationaleMessage = getString(R.string.permission_location_rationale),
                onGranted = {
                    // Get last location
                    model.getLocation(context) {
                        Timber.d("Got user location: $it")
                        if (it == null) {
                            context.toast(R.string.error_claim_gps_failed)
                        } else {
                            binding.mapView.getMapboxMap().setCamera(
                                CameraOptions.Builder()
                                    .zoom(USER_LOCATION_DEFAULT_ZOOM_LEVEL)
                                    .center(Point.fromLngLat(it.longitude, it.latitude))
                                    .build()
                            )
                        }
                    }
                },
                onDenied = { context.toast(R.string.error_claim_gps_failed) }
            )
        }
    }
}
