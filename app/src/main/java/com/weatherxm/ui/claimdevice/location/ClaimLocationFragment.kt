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
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.m5.ClaimM5ViewModel
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.toast
import com.weatherxm.util.hideKeyboard
import com.weatherxm.util.setHtml
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ClaimLocationFragment : Fragment() {
    private val m5ParentModel: ClaimM5ViewModel by activityViewModels()
    private val heliumParentModel: ClaimHeliumViewModel by activityViewModels()
    private val model: ClaimLocationViewModel by activityViewModels()
    private val navigator: Navigator by inject()
    private lateinit var binding: FragmentClaimSetLocationBinding

    companion object {
        const val TAG = "ClaimLocationFragment"
        const val ARG_DEVICE_TYPE = "has_pager"

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

        binding.cancel.setOnClickListener {
            if (model.getDeviceType() == DeviceType.M5_WIFI) {
                m5ParentModel.cancel()
            } else {
                heliumParentModel.cancel()
            }
        }

        binding.confirmAndClaim.setOnClickListener {
            if (model.getDeviceType() == DeviceType.M5_WIFI) {
                m5ParentModel.cancel()
            } else {
                heliumParentModel.cancel()
            }
        }

        binding.confirmAndClaim.setOnClickListener {
            // TODO: Confirm Location only if the installation togle is checked
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

        binding.addressSearchView.setAdapter(SearchResultsAdapter {
            model.getLocationFromSearchSuggestion(it)
            hideKeyboard()
        }) {
            model.geocoding(it)
        }

        binding.installationToggle.setOnCheckedChangeListener { _, checked ->
            with(binding) {
                if (checked) {
                    mapContainer.visibility = View.VISIBLE
                    infoContainer.strokeWidth = 0
                    needHelpInstallation.visibility = View.GONE
                    warningBox.visibility = View.GONE
                    toggleDescription.text = getString(R.string.installation_toggle_checked)
                    mapView.getFragment<ClaimMapFragment>().initMarkerAndListeners()
                } else {
                    mapContainer.visibility = View.GONE
                    infoContainer.strokeWidth = 1
                    needHelpInstallation.visibility = View.VISIBLE
                    warningBox.visibility = View.VISIBLE
                    toggleDescription.text = getString(R.string.installation_toggle_unchecked)
                }
            }
        }

        with(binding.needHelpInstallation) {
            movementMethod =
                me.saket.bettermovementmethod.BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, url ->
                        navigator.openWebsite(context, url)
                        return@setOnLinkClickListener true
                    }
                }
            setHtml(R.string.need_help_installation, getString(R.string.documentation_url))
        }
    }
}
