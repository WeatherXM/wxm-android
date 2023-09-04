package com.weatherxm.ui.explorer

import android.Manifest
import android.annotation.SuppressLint
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.google.android.material.search.SearchView.TransitionState
import com.google.firebase.analytics.FirebaseAnalytics
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.ui.BaseMapFragment
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.HEATMAP_SOURCE_ID
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.USER_LOCATION_DEFAULT_ZOOM_LEVEL
import com.weatherxm.ui.explorer.search.NetworkSearchResultsListAdapter
import com.weatherxm.ui.explorer.search.NetworkSearchViewModel
import com.weatherxm.ui.networkstats.NetworkStatsActivity
import com.weatherxm.util.Analytics
import com.weatherxm.util.Validator
import com.weatherxm.util.hideKeyboard
import com.weatherxm.util.onTextChanged
import dev.chrisbanes.insetter.applyInsetter
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber

class ExplorerMapFragment : BaseMapFragment(), KoinComponent {
    companion object {
        const val CAMERA_ANIMATION_DURATION = 400L
    }

    /*
        Use activityViewModels because we use this model to communicate with the parent activity
        so it needs to be the same model as the parent's one.
    */
    private val model: ExplorerViewModel by activityViewModels()
    private val searchModel: NetworkSearchViewModel by viewModels()
    private val analytics: Analytics by inject()
    private val validator: Validator by inject()
    private val navigator: Navigator by inject()

    private lateinit var adapter: NetworkSearchResultsListAdapter
    private var useSearchOnTextChangedListener = true

    override fun onMapReady(map: MapboxMap) {
        binding.appBar.applyInsetter {
            type(statusBars = true) {
                margin(left = false, top = true, right = false, bottom = false)
            }
        }

        adapter = NetworkSearchResultsListAdapter {
            binding.searchView.hide()
            model.onSearchOpenStatus(false)
            searchModel.onSearchClicked(it)
            it.center?.let { location ->
                cameraFly(Point.fromLngLat(location.lon, location.lat))
            }
            if (it.stationId != null) {
                navigator.showDeviceDetails(context, device = it.toUIDevice())
            }
            trackOnSearchResult(it.stationId != null)
        }
        binding.resultsRecycler.adapter = adapter

        binding.resultsRecycler.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                binding.searchView.hideKeyboard()
            }
        })

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

        getMapView().location.updateSettings {
            enabled = true
            pulsingEnabled = true
        }

        setSearchListeners()

        activity?.onBackPressedDispatcher?.addCallback(this, false) {
            if (model.onSearchOpenStatus().value == true) {
                binding.searchView.hide()
                model.onSearchOpenStatus(false)
            } else {
                activity?.finish()
            }
        }

        searchModel.onRecentSearches().observe(this) {
            handleRecentSearches(it)
        }

        model.onMyLocationClicked().observe(this) {
            if (it) {
                requestLocationPermissions()
                analytics.trackEventUserAction(
                    actionName = Analytics.ParamValue.MY_LOCATION.paramValue
                )
            }
        }

        searchModel.onSearchResults().observe(this) {
            onSearchResults(it)
        }

        model.explorerState().observe(this) {
            onExplorerState(map, it)
        }

        // Set camera to the last saved location the user was at
        model.getCurrentCamera()?.let {
            map.setCamera(CameraOptions.Builder().center(it.center).zoom(it.zoom).build())
        }

        // Fly the camera to the center of the hex selected
        model.onNavigateToLocation().observe(this) { location ->
            location?.let {
                cameraFly(Point.fromLngLat(it.lon, it.lat))
            }
        }

        // Fetch data
        model.fetch()
    }

    private fun handleRecentSearches(searchResults: List<SearchResult>) {
        if (searchResults.isEmpty()) {
            binding.resultsRecycler.setVisible(false)
            binding.searchEmptyResultsTitle.text = getString(R.string.search_no_recent_results)
            binding.searchEmptyResultsDesc.text =
                getString(R.string.search_no_recent_results_message)
            binding.searchEmptyResultsContainer.setVisible(true)
        } else {
            binding.resultsRecycler.setVisible(true)
            binding.searchEmptyResultsContainer.setVisible(false)
            adapter.updateData("", searchResults)
        }
    }

    private fun cameraFly(center: Point) {
        binding.mapView.getMapboxMap().flyTo(
            CameraOptions.Builder().zoom(USER_LOCATION_DEFAULT_ZOOM_LEVEL).center(center).build(),
            MapAnimationOptions.Builder().duration(CAMERA_ANIMATION_DURATION).build()
        )
    }

    private fun setSearchListeners() {
        binding.searchView.addTransitionListener { _, _, newState ->
            if (newState == TransitionState.SHOWING) {
                analytics.trackEventUserAction(
                    actionName = Analytics.ParamValue.EXPLORER_SEARCH.paramValue
                )
                analytics.trackScreen(
                    Analytics.Screen.NETWORK_SEARCH,
                    NetworkStatsActivity::class.simpleName
                )
                model.onSearchOpenStatus(true)
            } else if (newState == TransitionState.HIDING) {
                model.onSearchOpenStatus(false)
            }
            if (newState == TransitionState.SHOWN) {
                useSearchOnTextChangedListener = false
                binding.searchView.editText.setText(searchModel.getQuery())
                useSearchOnTextChangedListener = true
                if (searchModel.getQuery().isEmpty()) {
                    binding.recent.setVisible(true)
                    searchModel.getRecentSearches()
                }
            }
        }

        // This runs only when enter is clicked
        binding.searchView.editText.setOnEditorActionListener { _, _, keyEvent ->
            // When clicking enter on keyboard ignore ACTION_UP event (as we handled ACTION_DOWN)
            if (keyEvent?.action == ACTION_UP && keyEvent.keyCode == KEYCODE_ENTER) {
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener !validateAndSearch()
        }

        binding.searchView.editText.onTextChanged {
            if (binding.searchView.currentTransitionState == TransitionState.SHOWN
                && useSearchOnTextChangedListener
            ) {
                searchModel.setQuery(it)
                if (validator.validateNetworkSearchQuery(it)) {
                    searchModel.networkSearch()
                    return@onTextChanged
                }
                searchModel.cancelNetworkSearchJob()
                binding.searchProgress.visibility = View.INVISIBLE
                if (it.isEmpty()) {
                    binding.recent.setVisible(true)
                    searchModel.getRecentSearches()
                }
            }
        }

        binding.searchBar.setOnMenuItemClickListener {
            onSearchBarMenuItem(it)
        }
    }

    private fun onSearchBarMenuItem(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.settings -> {
                analytics.trackEventSelectContent(
                    contentType = Analytics.ParamValue.EXPLORER_SETTINGS.paramValue
                )
                navigator.showPreferences(this)
                true
            }

            else -> false
        }
    }

    // Returns if validation was a success and search API call has been processed
    private fun validateAndSearch(): Boolean {
        val query = binding.searchView.text.toString()
        return if (validator.validateNetworkSearchQuery(query)) {
            searchModel.networkSearch(true)
            true
        } else {
            context?.toast(R.string.network_search_validation_error)
            false
        }
    }

    private fun onSearchResults(resource: Resource<List<SearchResult>>) {
        when (resource.status) {
            Status.SUCCESS -> {
                binding.searchResultsStatusView.setVisible(false)
                if (resource.data.isNullOrEmpty()) {
                    binding.resultsRecycler.setVisible(false)
                    binding.searchEmptyResultsContainer.setVisible(true)
                    binding.searchEmptyResultsTitle.text = getString(R.string.search_no_results)
                    binding.searchEmptyResultsDesc.text =
                        getString(R.string.search_no_results_message)
                } else {
                    binding.resultsRecycler.setVisible(true)
                    binding.searchEmptyResultsContainer.setVisible(false)
                    adapter.updateData(binding.searchView.text.toString(), resource.data)
                }
                binding.searchProgress.visibility = View.INVISIBLE
            }
            Status.ERROR -> {
                binding.searchProgress.visibility = View.INVISIBLE
                binding.resultsRecycler.setVisible(false)
                binding.searchEmptyResultsContainer.setVisible(false)
                binding.searchResultsStatusView
                    .clear()
                    .animation(R.raw.anim_error)
                    .title(getString(R.string.search_error))
                    .subtitle(resource.message)
                    .action(getString(R.string.action_retry))
                    .listener {
                        validateAndSearch()
                    }
                    .setVisible(true)
            }
            Status.LOADING -> {
                binding.searchResultsStatusView.setVisible(false)
                binding.recent.setVisible(false)
                binding.searchEmptyResultsContainer.setVisible(false)
                binding.searchProgress.visibility = View.VISIBLE
            }
        }
    }

    private fun onExplorerState(map: MapboxMap, resource: Resource<ExplorerData>) {
        Timber.d("Data updated: ${resource.status}")
        when (resource.status) {
            Status.SUCCESS -> {
                if (resource.data?.geoJsonSource == null) {
                    onPolygonPointsUpdated(resource.data?.polygonPoints)
                    binding.progress.visibility = View.INVISIBLE
                    return
                }

                val mapStyle = map.getStyle()
                if (mapStyle?.styleSourceExists(HEATMAP_SOURCE_ID) == true) {
                    resource.data.geoJsonSource.data?.let {
                        (mapStyle.getSource(HEATMAP_SOURCE_ID) as GeoJsonSource).data(it)
                    }
                } else {
                    mapStyle?.addSource(resource.data.geoJsonSource)
                    mapStyle?.addLayerAbove(model.getHeatMapLayer(), "waterway-label")
                }
                onPolygonPointsUpdated(resource.data.polygonPoints)
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

    override fun onResume() {
        super.onResume()
        binding.searchBar.menu.getItem(0).isVisible = !model.isExplorerAfterLoggedIn()
        if (model.isExplorerAfterLoggedIn()) {
            analytics.trackScreen(
                Analytics.Screen.EXPLORER,
                ExplorerMapFragment::class.simpleName
            )
        } else {
            analytics.trackScreen(
                Analytics.Screen.EXPLORER_LANDING,
                ExplorerMapFragment::class.simpleName
            )
        }
    }

    private fun trackOnSearchResult(isStationResult: Boolean) {
        val itemId = if (binding.recent.isVisible) {
            Analytics.ParamValue.RECENT.paramValue
        } else {
            Analytics.ParamValue.SEARCH.paramValue
        }

        val resultType = if (isStationResult) {
            Analytics.ParamValue.STATION.paramValue
        } else {
            Analytics.ParamValue.LOCATION.paramValue
        }

        analytics.trackEventSelectContent(
            Analytics.ParamValue.NETWORK_SEARCH.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, itemId),
            Pair(FirebaseAnalytics.Param.ITEM_LIST_ID, resultType)
        )
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
                            cameraFly(Point.fromLngLat(it.lon, it.lat))
                        }
                    }
                },
                onDenied = { context.toast(R.string.error_claim_gps_failed) }
            )
        }
    }
}
