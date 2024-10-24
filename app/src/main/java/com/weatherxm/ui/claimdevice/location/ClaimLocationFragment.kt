package com.weatherxm.ui.claimdevice.location

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
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
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.EditLocationListener
import com.weatherxm.ui.components.EditLocationMapFragment
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimLocationFragment : BaseFragment(), EditLocationListener {
    private val model: ClaimLocationViewModel by activityViewModel()
    private val heliumParentModel: ClaimHeliumViewModel by activityViewModel()
    private val wifiParentModel: ClaimWifiViewModel by activityViewModel()
    private val pulseParentModel: ClaimPulseViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimSetLocationBinding

    companion object {
        const val TAG = "ClaimLocationFragment"

        fun newInstance(deviceType: DeviceType) = ClaimLocationFragment().apply {
            arguments = Bundle().apply { putParcelable(ARG_DEVICE_TYPE, deviceType) }
        }
    }

    init {
        lifecycleScope.launch {
            whenCreated {
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
            val markerLocation = getMapFragment().getMarkerLocation()
            if (!model.validateLocation(markerLocation.lat, markerLocation.lon)) {
                showSnackbarMessage(
                    binding.root,
                    getString(R.string.invalid_location),
                    callback = { snackbar?.dismiss() },
                    R.string.action_dismiss,
                    binding.confirm
                )
                return@setOnClickListener
            }
            model.setInstallationLocation(markerLocation.lat, markerLocation.lon)

            when (model.getDeviceType()) {
                DeviceType.M5_WIFI, DeviceType.D1_WIFI -> wifiParentModel.next()
                DeviceType.PULSE_4G -> pulseParentModel.next()
                DeviceType.HELIUM -> heliumParentModel.next()
            }
        }

        val adapter = SearchResultsAdapter {
            model.getLocationFromSearchSuggestion(it)
            hideKeyboard()

            analytics.trackEventUserAction(
                actionName = AnalyticsService.ParamValue.SEARCH_LOCATION.paramValue,
                contentType = AnalyticsService.ParamValue.CLAIMING_ADDRESS_SEARCH.paramValue,
                Pair(FirebaseAnalytics.Param.LOCATION, it.name)
            )
        }

        binding.addressSearchView.setAdapter(adapter,
            onTextChanged = { model.getSearchSuggestions(it) },
            onMyLocationClicked = {
                requestLocationPermissions(activity) {
                    model.getLocation()
                }
            }
        )
    }

    private fun getMapFragment(): EditLocationMapFragment {
        return binding.mapView.getFragment()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady() {
        getMapFragment().addOnMapIdleListener {
            model.getAddressFromPoint(it)
        }

        model.onRequestUserLocation().observe(viewLifecycleOwner) {
            if (it) {
                requestLocationPermissions(activity) {
                    model.getLocation()
                }
            }
        }

        val addressSearchView = if (model.getDeviceType() == DeviceType.HELIUM) {
            binding.addressSearchView
        } else {
            binding.addressSearchView
        }
        model.onSearchResults().observe(viewLifecycleOwner) {
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

        model.onReverseGeocodedAddress().observe(viewLifecycleOwner) {
            getMapFragment().showMarkerAddress(it)
        }

        model.onMoveToLocation().observe(viewLifecycleOwner) {
            getMapFragment().moveToLocation(it)
            addressSearchView.clear()
        }

        getMapFragment().initMarkerAndListeners()
    }
}
