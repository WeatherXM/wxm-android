package com.weatherxm.ui.claimdevice.location

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimSetLocationBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.m5.ClaimM5ViewModel
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.SearchResultsAdapter
import com.weatherxm.ui.common.hideKeyboard
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.EditLocationListener
import com.weatherxm.ui.components.EditLocationMapFragment
import com.weatherxm.util.Analytics
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimLocationFragment : BaseFragment(), EditLocationListener {
    private val model: ClaimLocationViewModel by activityViewModel()
    private val heliumParentModel: ClaimHeliumViewModel by activityViewModel()
    private val m5ParentModel: ClaimM5ViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimSetLocationBinding

    companion object {
        const val TAG = "ClaimLocationFragment"
        const val ARG_DEVICE_TYPE = "device_type"

        fun newInstance(deviceType: DeviceType) = ClaimLocationFragment().apply {
            arguments = Bundle().apply { putSerializable(ARG_DEVICE_TYPE, deviceType) }
        }
    }

    init {
        lifecycleScope.launch {
            whenCreated {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arguments?.getSerializable(ARG_DEVICE_TYPE, DeviceType::class.java)?.let {
                        model.setDeviceType(it)
                    }
                } else {
                    arguments?.getSerializable(ARG_DEVICE_TYPE)?.let {
                        model.setDeviceType(it as DeviceType)
                    }
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
                activity?.toast(R.string.invalid_location)
                return@setOnClickListener
            }
            model.setInstallationLocation(markerLocation.lat, markerLocation.lon)

            if (model.getDeviceType() == DeviceType.M5_WIFI) {
                m5ParentModel.next()
            } else {
                heliumParentModel.next()
            }
        }

        val adapter = SearchResultsAdapter {
            model.getLocationFromSearchSuggestion(it)
            hideKeyboard()

            analytics.trackEventUserAction(
                actionName = Analytics.ParamValue.SEARCH_LOCATION.paramValue,
                contentType = Analytics.ParamValue.CLAIMING_ADDRESS_SEARCH.paramValue,
                Pair(FirebaseAnalytics.Param.LOCATION, it.name)
            )
        }

        binding.addressSearchView.setAdapter(adapter,
            onTextChanged = { model.geocoding(it) },
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

        model.onSearchResults().observe(viewLifecycleOwner) {
            if (it == null) {
                context?.toast(getString(R.string.error_search_suggestions))
            } else if (it.isEmpty() || binding.addressSearchView.getQueryLength() <= 2) {
                binding.addressSearchView.clear()
            } else {
                binding.addressSearchView.setData(it)
            }
        }

        model.onReverseGeocodedAddress().observe(viewLifecycleOwner) {
            getMapFragment().showMarkerAddress(it)
        }

        model.onMoveToLocation().observe(viewLifecycleOwner) {
            getMapFragment().moveToLocation(it)
            binding.addressSearchView.clear()
        }

        getMapFragment().initMarkerAndListeners()
    }
}
