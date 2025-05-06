package com.weatherxm.ui.explorer

import android.annotation.SuppressLint
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageButton
import android.widget.PopupWindow
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.google.android.material.card.MaterialCardView
import com.google.android.material.search.SearchView.TransitionState
import com.google.firebase.analytics.FirebaseAnalytics
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.toCameraOptions
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.hideKeyboard
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.onTextChanged
import com.weatherxm.ui.common.show
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseMapFragment
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.HEATMAP_SOURCE_ID
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.POINT_LAYER
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.SHOW_STATION_COUNT_ZOOM_LEVEL
import com.weatherxm.ui.explorer.search.NetworkSearchResultsListAdapter
import com.weatherxm.ui.explorer.search.NetworkSearchViewModel
import com.weatherxm.ui.networkstats.NetworkStatsActivity
import com.weatherxm.util.MapboxUtils
import com.weatherxm.util.NumberUtils.formatNumber
import com.weatherxm.util.Validator
import dev.chrisbanes.insetter.applyInsetter
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

@Suppress("TooManyFunctions")
class ExplorerMapFragment : BaseMapFragment() {
    companion object {
        const val CAMERA_ANIMATION_DURATION = 400L
        const val STATION_COUNT_POINT_TEXT_SIZE = 16.0
    }

    /*
        Use activityViewModel because we use this model to communicate with the parent activity
        so it needs to be the same model as the parent's one.
    */
    private val model: ExplorerViewModel by activityViewModel()
    private val searchModel: NetworkSearchViewModel by viewModel()

    private lateinit var adapter: NetworkSearchResultsListAdapter
    private var useSearchOnTextChangedListener = true

    override fun onMapReady(map: MapboxMap) {
        binding.topBar.applyInsetter {
            type(statusBars = true) {
                margin(left = false, top = true, right = false, bottom = false)
            }
        }

        adapter = NetworkSearchResultsListAdapter {
            onNetworkSearchResultClicked(it)
        }
        binding.resultsRecycler.adapter = adapter

        binding.resultsRecycler.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                binding.searchView.hideKeyboard()
            }
        })

        polygonManager.addClickListener {
            MapboxUtils.getCustomData(it)?.let { cell -> navigator.showCellInfo(context, cell) }
            true
        }

        map.getLayer(POINT_LAYER)?.minZoom(SHOW_STATION_COUNT_ZOOM_LEVEL)

        map.addOnMapClickListener {
            model.onMapClick()
            true
        }

        map.subscribeCameraChanged {
            model.setCurrentCamera(it.cameraState.zoom, it.cameraState.center)
        }

        map.subscribeMapIdle {
            with(map.coordinateBoundsForCamera(map.cameraState.toCameraOptions())) {
                model.getActiveStationsInViewPort(north(), south(), east(), west())
            }
        }

        getMapView().location.updateSettings { enabled = true }

        setSearchListeners()

        activity?.onBackPressedDispatcher?.addCallback {
            if (model.onSearchOpenStatus().value == true) {
                binding.searchView.hide()
                model.onSearchOpenStatus(false)
            } else {
                if (model.isExplorerAfterLoggedIn()) {
                    findNavController().popBackStack()
                } else {
                    activity?.finish()
                }
            }
        }

        searchModel.onRecentSearches().observe(this) {
            handleRecentSearches(it)
        }

        model.onMyLocationClicked().observe(this) {
            onMyLocationClicked(it)
        }

        searchModel.onSearchResults().observe(this) {
            onSearchResults(it)
        }

        model.onStatus().observe(this) {
            onStatus(it.status)
        }

        model.onExplorerData().observe(this) {
            onExplorerData(map, it)
        }

        model.onNewPolygons().observe(this) {
            onPolygonPointsUpdated(it)
        }

        // Set camera to the last saved location the user was at
        model.getCurrentCamera()?.let {
            map.setCamera(CameraOptions.Builder().center(it.center).zoom(it.zoom).build())
        }

        // Fly the camera to the center of the hex selected
        model.onNavigateToLocation().observe(this) { location ->
            location?.let {
                cameraFly(Point.fromLngLat(it.location.lon, it.location.lat), it.zoomLevel)
            }
        }

        model.onViewportStations().observe(this) {
            binding.activeStations.text = formatNumber(it)
        }

        binding.menuBtn.setOnClickListener {
            setupMenu()
        }

        // Fetch data
        model.fetch()
    }

    private fun handleRecentSearches(searchResults: List<SearchResult>) {
        if (searchResults.isEmpty()) {
            binding.resultsRecycler.visible(false)
            binding.searchEmptyResultsTitle.text = getString(R.string.search_no_recent_results)
            binding.searchEmptyResultsDesc.text =
                getString(R.string.search_no_recent_results_message)
            binding.searchEmptyResultsContainer.visible(true)
        } else {
            binding.resultsRecycler.visible(true)
            binding.searchEmptyResultsContainer.visible(false)
            adapter.updateData(String.empty(), searchResults)
        }
    }

    private fun onMyLocationClicked(isClicked: Boolean?) {
        if (isClicked == true) {
            getLocationPermissions()
            analytics.trackEventUserAction(AnalyticsService.ParamValue.MY_LOCATION.paramValue)
        }
    }

    private fun onNetworkSearchResultClicked(networkSearchResult: SearchResult) {
        binding.searchView.hide()
        model.onSearchOpenStatus(false)
        searchModel.onSearchClicked(networkSearchResult)
        networkSearchResult.center?.let { location ->
            cameraFly(Point.fromLngLat(location.lon, location.lat))
        }
        if (networkSearchResult.stationId != null) {
            navigator.showDeviceDetails(context, device = networkSearchResult.toUIDevice())
        }
        trackOnSearchResult(networkSearchResult.stationId != null)
    }

    private fun cameraFly(center: Point, zoomLevel: Double = ZOOMED_IN_ZOOM_LEVEL) {
        getMap().flyTo(
            CameraOptions.Builder().zoom(zoomLevel).center(center).build(),
            MapAnimationOptions.Builder().duration(CAMERA_ANIMATION_DURATION).build()
        )
    }

    private fun setSearchListeners() {
        binding.searchBtn.setOnClickListener {
            binding.searchView.show()
        }

        binding.searchView.addTransitionListener { _, _, newState ->
            if (newState == TransitionState.SHOWING) {
                analytics.trackEventUserAction(
                    actionName = AnalyticsService.ParamValue.EXPLORER_SEARCH.paramValue
                )
                analytics.trackScreen(
                    AnalyticsService.Screen.NETWORK_SEARCH,
                    NetworkStatsActivity::class.simpleName ?: String.empty()
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
                    binding.recent.visible(true)
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
                if (Validator.validateNetworkSearchQuery(it)) {
                    searchModel.networkSearch()
                    return@onTextChanged
                }
                searchModel.cancelNetworkSearchJob()
                binding.searchProgress.invisible()
                if (it.isEmpty()) {
                    binding.recent.visible(true)
                    searchModel.getRecentSearches()
                }
            }
        }
    }

    // Returns if validation was a success and search API call has been processed
    private fun validateAndSearch(): Boolean {
        val query = binding.searchView.text.toString()
        return if (Validator.validateNetworkSearchQuery(query)) {
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
                binding.searchResultsStatusView.visible(false)
                if (resource.data.isNullOrEmpty()) {
                    binding.resultsRecycler.visible(false)
                    binding.searchEmptyResultsContainer.visible(true)
                    binding.searchEmptyResultsTitle.text = getString(R.string.search_no_results)
                    binding.searchEmptyResultsDesc.text =
                        getString(R.string.search_no_results_message)
                } else {
                    binding.resultsRecycler.visible(true)
                    binding.searchEmptyResultsContainer.visible(false)
                    adapter.updateData(binding.searchView.text.toString(), resource.data)
                }
                binding.searchProgress.invisible()
            }
            Status.ERROR -> {
                binding.searchProgress.invisible()
                binding.resultsRecycler.visible(false)
                binding.searchEmptyResultsContainer.visible(false)
                binding.searchResultsStatusView
                    .clear()
                    .animation(R.raw.anim_error)
                    .title(getString(R.string.search_error))
                    .subtitle(resource.message)
                    .action(getString(R.string.action_retry))
                    .listener {
                        validateAndSearch()
                    }
                    .visible(true)
            }
            Status.LOADING -> {
                binding.searchResultsStatusView.visible(false)
                binding.recent.visible(false)
                binding.searchEmptyResultsContainer.visible(false)
                binding.searchProgress.visible(true)
            }
        }
    }

    private fun onStatus(status: Status) {
        Timber.d("Data updated: $status")
        when (status) {
            Status.SUCCESS -> {
                binding.progress.invisible()
            }
            Status.ERROR -> {
                binding.progress.invisible()
            }
            Status.LOADING -> {
                binding.progress.visible(true)
            }
        }
    }

    private fun onExplorerData(map: MapboxMap, data: ExplorerData) {
        val mapStyle = map.style
        if (mapStyle?.styleSourceExists(HEATMAP_SOURCE_ID) == true) {
            data.geoJsonSource.data?.let {
                (mapStyle.getSource(HEATMAP_SOURCE_ID) as GeoJsonSource).data(it)
            }
        } else {
            mapStyle?.addSource(data.geoJsonSource)
            mapStyle?.addLayerAbove(model.heatmapLayer, "waterway-label")
        }
        onPolygonPointsUpdated(data.polygonsToDraw)
        onPointsUpdated(data.pointsToDraw)
    }

    override fun onResume() {
        super.onResume()
        if (model.isExplorerAfterLoggedIn()) {
            analytics.trackScreen(AnalyticsService.Screen.EXPLORER, classSimpleName())
        } else {
            analytics.trackScreen(AnalyticsService.Screen.EXPLORER_LANDING, classSimpleName())
        }
    }

    private fun trackOnSearchResult(isStationResult: Boolean) {
        val itemId = if (binding.recent.isVisible) {
            AnalyticsService.ParamValue.RECENT.paramValue
        } else {
            AnalyticsService.ParamValue.SEARCH.paramValue
        }

        val resultType = if (isStationResult) {
            AnalyticsService.ParamValue.STATION.paramValue
        } else {
            AnalyticsService.ParamValue.LOCATION.paramValue
        }

        analytics.trackEventSelectContent(
            AnalyticsService.ParamValue.NETWORK_SEARCH.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, itemId),
            Pair(FirebaseAnalytics.Param.ITEM_LIST_ID, resultType)
        )
    }

    private fun onPolygonPointsUpdated(polygonsToDraw: List<PolygonAnnotationOptions>?) {
        if (polygonsToDraw.isNullOrEmpty()) {
            Timber.d("No new polygons found. Skipping map update.")
            return
        }

        polygonManager.create(polygonsToDraw)
    }

    private fun onPointsUpdated(pointsToDraw: List<PointAnnotationOptions>) {
        if (pointsToDraw.isEmpty()) {
            Timber.d("No new points found. Skipping map update.")
            return
        }

        pointManager.create(pointsToDraw)
    }

    override fun getMapStyle(): String {
        /**
         * If a custom mapbox style is available (which are set through env param MAPBOX_STYLE)
         * use it otherwise use the default ones found in BaseMapFragment
         */
        return getString(R.string.mapbox_style).ifEmpty {
            super.getMapStyle()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationPermissions() {
        requestLocationPermissions(activity) {
            // Get last location
            model.getLocation {
                Timber.d("Got user location: $it")
                if (it == null) {
                    context.toast(R.string.error_claim_gps_failed)
                } else {
                    cameraFly(Point.fromLngLat(it.lon, it.lat))
                }
            }
        }
    }

    @Suppress("MagicNumber")
    private fun setupMenu() {
        val popupView = layoutInflater.inflate(R.layout.view_map_menu, binding.root, false)
        val popupWindow = PopupWindow(popupView, MATCH_PARENT, WRAP_CONTENT, true)

        /**
         * Create translucent background for popup window
         */
        View(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            context?.getColor(R.color.translucent_black_darker)?.let {
                setBackgroundColor(it)
            }
        }.apply {
            val rootLayout = activity?.window?.decorView as ViewGroup
            rootLayout.addView(this)

            popupWindow.setOnDismissListener {
                rootLayout.removeView(this)
            }
        }

        /**
         * Show the popup aligned to the anchor (the 3-dots button)
         */
        popupWindow.showAsDropDown(
            binding.menuBtn,
            0,
            context?.resources?.getDimension(R.dimen.margin_normal)?.toInt() ?: 16
        )

        popupView.findViewById<ImageButton>(R.id.closeBtn).setOnClickListener {
            popupWindow.dismiss()
        }

        popupView.findViewById<MaterialCardView>(R.id.networkStatsContainer).setOnClickListener {
            navigator.showNetworkStats(context)
            popupWindow.dismiss()
        }

        if (!model.isExplorerAfterLoggedIn()) {
            popupView.findViewById<MaterialCardView>(R.id.settingsContainer).apply {
                setOnClickListener {
                    analytics.trackEventSelectContent(
                        contentType = AnalyticsService.ParamValue.EXPLORER_SETTINGS.paramValue
                    )
                    navigator.showPreferences(this@ExplorerMapFragment)
                    popupWindow.dismiss()
                }
                show()
            }
        }
    }
}
