package com.weatherxm.ui.claimdevice.location

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.FragmentClaimSetLocationBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseViewModel
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiViewModel
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_TYPE
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.SearchResultsAdapter
import com.weatherxm.ui.common.hideKeyboard
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.EditLocationListener
import com.weatherxm.ui.components.EditLocationMapFragment
import com.weatherxm.ui.deviceeditlocation.DeviceEditLocationViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ClaimLocationFragment : BaseFragment(), EditLocationListener {
    private val model: ClaimLocationViewModel by activityViewModel()
    private val heliumParentModel: ClaimHeliumViewModel by activityViewModel()
    private val wifiParentModel: ClaimWifiViewModel by activityViewModel()
    private val pulseParentModel: ClaimPulseViewModel by activityViewModel()
    private val editLocationViewModel: DeviceEditLocationViewModel by viewModel()
    private lateinit var binding: FragmentClaimSetLocationBinding

    companion object {
        const val TAG = "ClaimLocationFragment"

        fun newInstance(deviceType: DeviceType) = ClaimLocationFragment().apply {
            arguments = Bundle().apply { putParcelable(ARG_DEVICE_TYPE, deviceType) }
        }
    }

    init {
        lifecycleScope.launch {
            withCreated {
                arguments?.parcelable<DeviceType>(ARG_DEVICE_TYPE)?.let {
                    model.setDeviceType(it)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimSetLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (context == null) {
            // No point executing if in the meanwhile the activity is dead
            return
        }

        getMapFragment().setListener(this)

        binding.confirmLocationToggle.setOnCheckedChangeListener { _, checked ->
            binding.confirm.isEnabled = checked
        }

        binding.confirm.setOnClickListener {
            onConfirmClicked()
        }

        val adapter = SearchResultsAdapter {
            editLocationViewModel.getLocationFromSearchSuggestion(it)
            hideKeyboard()

            analytics.trackEventUserAction(
                actionName = AnalyticsService.ParamValue.SEARCH_LOCATION.paramValue,
                contentType = AnalyticsService.ParamValue.CLAIMING_ADDRESS_SEARCH.paramValue,
                Pair(FirebaseAnalytics.Param.LOCATION, it.name)
            )
        }

        binding.addressSearchView.setAdapter(
            adapter,
            onTextChanged = { editLocationViewModel.getSearchSuggestions(it) },
            onMyLocationClicked = {
                requestLocationPermissions(activity) {
                    editLocationViewModel.getLocation()
                }
            }
        )

        editLocationViewModel.getCells()
    }

    private fun getMapFragment(): EditLocationMapFragment {
        return binding.mapView.getFragment()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady() {
        getMapFragment().addOnMapIdleListener { point ->
            point?.let {
                editLocationViewModel.getAddressFromPoint(it)
                editLocationViewModel.isPointOnBelowCapacityCell(it.latitude(), it.longitude())
            }
        }

        model.onRequestUserLocation().observe(viewLifecycleOwner) {
            if (it) {
                requestLocationPermissions(activity) {
                    editLocationViewModel.getLocation()
                }
            }
        }

        val addressSearchView = if (model.getDeviceType() == DeviceType.HELIUM) {
            binding.addressSearchView
        } else {
            binding.addressSearchView
        }
        editLocationViewModel.onSearchResults().observe(viewLifecycleOwner) {
            if (it == null) {
                showSnackbarMessage(
                    binding.root,
                    getString(R.string.error_search_suggestions),
                    callback = { snackbar?.dismiss() },
                    R.string.action_dismiss,
                    binding.confirm
                )
            } else if (it.isEmpty() || addressSearchView.getQueryLength() <= 2) {
                addressSearchView.clear()
            } else {
                addressSearchView.setData(it)
            }
        }

        editLocationViewModel.onReverseGeocodedAddress().observe(viewLifecycleOwner) {
            getMapFragment().showMarkerAddress(it)
        }

        editLocationViewModel.onMoveToLocation().observe(viewLifecycleOwner) {
            getMapFragment().moveToLocation(it)
            addressSearchView.clear()
        }

        editLocationViewModel.onCapacityLayer().observe(this) { layerData ->
            layerData?.let {
                getMapFragment().drawCapacityLayers(it)
            }
        }

        editLocationViewModel.onCellWithBelowCapacity().observe(this) { isBelowCapacity ->
            if (isBelowCapacity == null || isBelowCapacity) {
                snackbar?.dismiss()
            } else {
                showSnackbarMessage(
                    viewGroup = binding.root,
                    message = getString(R.string.cell_already_max_capacity),
                    callback = {
                        navigator.openWebsite(
                            context,
                            getString(R.string.docs_url_cell_capacity)
                        )
                        snackbar?.dismiss()
                    },
                    actionTextResId = R.string.read_more,
                    anchorView = binding.confirmLocationToggle
                )
            }
        }

        getMapFragment().initMarkerAndListeners()
    }

    private fun onConfirmClicked() {
        val markerLocation = getMapFragment().getMarkerLocation()
        /**
         * Check if the location is valid
         */
        if (!editLocationViewModel.validateLocation(markerLocation.lat, markerLocation.lon)) {
            showSnackbarMessage(
                binding.root,
                getString(R.string.invalid_location),
                callback = { snackbar?.dismiss() },
                R.string.action_dismiss,
                binding.confirm
            )
            return
        }

        /**
         * Check if the location is inside a cell which has available capacity to proceed
         * otherwise show a confirmation dialog
         */
        if (editLocationViewModel.onCellWithBelowCapacity().value == false) {
            ActionDialogFragment
                .Builder(
                    title = getString(R.string.watch_out),
                    message = getString(R.string.watch_out_cell_capacity),
                    positive = getString(R.string.relocate)
                )
                .onNegativeClick(getString(R.string.proceed_anyway)) {
                    snackbar?.dismiss()
                    setLocationAndProceed(markerLocation.lat, markerLocation.lon)
                }
                .build()
                .show(this)
        } else {
            setLocationAndProceed(markerLocation.lat, markerLocation.lon)
        }
    }

    private fun setLocationAndProceed(lat: Double, lon: Double) {
        model.setInstallationLocation(lat, lon)

        when (model.getDeviceType()) {
            DeviceType.M5_WIFI, DeviceType.D1_WIFI -> wifiParentModel.next()
            DeviceType.PULSE_4G -> pulseParentModel.next()
            DeviceType.HELIUM -> heliumParentModel.next()
        }
    }
}
