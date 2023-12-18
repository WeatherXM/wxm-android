package com.weatherxm.ui.deviceeditlocation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityDeviceEditLocationBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.claimdevice.location.SearchResultsAdapter
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.requestLocationPermissions
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.util.applyInsets
import com.weatherxm.util.hideKeyboard
import com.weatherxm.util.setHtml
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class DeviceEditLocationActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityDeviceEditLocationBinding
    private val model: DeviceEditLocationViewModel by viewModels()
    private val navigator: Navigator by inject()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceEditLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        val device = intent?.extras?.getParcelable<UIDevice>(ARG_DEVICE)
        if (device == null || device.isEmpty()) {
            Timber.d("Could not start DeviceEditLocationActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        with(binding.toolbar) {
            setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            subtitle = device.getDefaultOrFriendlyName()
        }

        binding.confirmLocationToggle.setOnCheckedChangeListener { _, isChecked ->
            binding.confirmBtn.isEnabled = isChecked
        }

        binding.confirmBtn.setOnClickListener {
            binding.mapView.getFragment<DeviceEditLocationMapFragment>().onConfirmClicked(device.id)
        }

        with(binding.relocationFeeNotice) {
            movementMethod =
                me.saket.bettermovementmethod.BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, url ->
                        navigator.openWebsite(context, url)
                        return@setOnLinkClickListener true
                    }
                }
            setHtml(R.string.relocation_fee_notice, getString(R.string.docs_url_pol_algorithm))
        }

        model.onSelectedSearchLocation().observe(this) {
            binding.addressSearchView.clear()
        }

        model.onSearchResults().observe(this) {
            if (it == null) {
                toast(getString(R.string.error_search_suggestions))
            } else if (it.isEmpty() || binding.addressSearchView.getQueryLength() <= 2) {
                binding.addressSearchView.clear()
            } else {
                binding.addressSearchView.setData(it)
            }
        }

        model.onUpdatedDevice().observe(this) {
            onUpdatedDevice(it)
        }

        val adapter = SearchResultsAdapter {
            model.getLocationFromSearchSuggestion(it)
            hideKeyboard()
        }

        binding.addressSearchView.setAdapter(
            adapter,
            onTextChanged = { model.geocoding(it) },
            onMyLocationClicked = {
                requestLocationPermissions {
                    model.getLocation()
                }
            }
        )

        binding.mapView.getFragment<DeviceEditLocationMapFragment>().initMarkerAndListeners()
        model.goToLocation(device.location)
    }

    private fun onUpdatedDevice(resource: Resource<UIDevice>) {
        binding.progress.setVisible(resource.status == Status.LOADING)
        binding.confirmBtn.isEnabled = resource.status == Status.ERROR

        if (resource.status == Status.SUCCESS) {
            toast(R.string.location_updated)
            val resultValue = Intent().putExtra(ARG_DEVICE, resource.data)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        } else if (resource.status == Status.ERROR) {
            resource.message?.let { toast(it, Toast.LENGTH_LONG) }
        }
    }
}
