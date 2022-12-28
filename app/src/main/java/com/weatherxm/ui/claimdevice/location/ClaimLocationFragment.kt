package com.weatherxm.ui.claimdevice.location

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimSetLocationBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.m5.ClaimM5ViewModel
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.toast
import com.weatherxm.util.hideKeyboard
import kotlinx.coroutines.launch

class ClaimLocationFragment : Fragment() {
    private val m5ParentModel: ClaimM5ViewModel by activityViewModels()
    private val heliumParentModel: ClaimHeliumViewModel by activityViewModels()
    private val model: ClaimLocationViewModel by activityViewModels()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (context == null) {
            // No point executing if in the meanwhile the activity is dead
            return
        }

        binding.confirmLocationToggle.setOnCheckedChangeListener { _, checked ->
            binding.confirm.isEnabled = checked
        }

        binding.confirm.setOnClickListener {
            model.confirmLocation()
            if (model.getDeviceType() == DeviceType.M5_WIFI) {
                m5ParentModel.next()
            } else {
                heliumParentModel.next()
            }
        }

        model.onSelectedSearchLocation().observe(viewLifecycleOwner) {
            binding.addressSearchView.clear()
        }

        model.onSearchResults().observe(viewLifecycleOwner) {
            if (it == null) {
                context.toast(getString(R.string.error_search_suggestions))
            } else if (it.isEmpty() || binding.addressSearchView.getQueryLength() <= 2) {
                binding.addressSearchView.clear()
            } else {
                binding.addressSearchView.setData(it)
            }
        }

        val adapter = SearchResultsAdapter {
            model.getLocationFromSearchSuggestion(it)
            hideKeyboard()
        }

        binding.addressSearchView.setAdapter(adapter,
            onTextChanged = {
                model.geocoding(it)
            },
            onMyLocationClicked = {
                model.getUserLocation()
            }
        )

        binding.mapView.getFragment<ClaimMapFragment>().initMarkerAndListeners()
    }
}
