package com.weatherxm.ui.deviceeditlocation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.weatherxm.R
import com.weatherxm.databinding.ActivityDeviceEditLocationBinding
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.SearchResultsAdapter
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.hideKeyboard
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.EditLocationListener
import com.weatherxm.ui.components.EditLocationMapFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class DeviceEditLocationActivity : BaseActivity(), EditLocationListener {
    private lateinit var binding: ActivityDeviceEditLocationBinding
    private val model: DeviceEditLocationViewModel by viewModel()
    private var device: UIDevice? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceEditLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val device = intent?.extras?.parcelable<UIDevice>(ARG_DEVICE)
        if (device == null || device.isEmpty() || device.relation != DeviceRelation.OWNED) {
            Timber.d("Could not start DeviceEditLocationActivity. Device is null or not owned.")
            toast(R.string.error_generic_message)
            finish()
            return
        }
        this.device = device

        with(binding.toolbar) {
            setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            subtitle = device.getDefaultOrFriendlyName()
        }

        getMapFragment().setListener(this)

        binding.confirmLocationToggle.setOnCheckedChangeListener { _, isChecked ->
            binding.confirmBtn.isEnabled = isChecked
        }

        binding.confirmBtn.setOnClickListener {
            val markerLocation = getMapFragment().getMarkerLocation()
            if (!model.validateLocation(markerLocation.lat, markerLocation.lon)) {
                toast(R.string.invalid_location)
                return@setOnClickListener
            }

            /**
             * Check if the location is inside a cell which has available capacity to proceed
             * otherwise show a confirmation dialog
             */
            if (model.onCellWithBelowCapacity().value == false) {
                ActionDialogFragment
                    .Builder(
                        title = getString(R.string.watch_out),
                        message = getString(R.string.watch_out_cell_capacity),
                        positive = getString(R.string.relocate)
                    )
                    .onNegativeClick(getString(R.string.proceed_anyway)) {
                        snackbar?.dismiss()
                        model.setLocation(device.id, markerLocation.lat, markerLocation.lon)
                    }
                    .build()
                    .show(this)
            } else {
                snackbar?.dismiss()
                model.setLocation(device.id, markerLocation.lat, markerLocation.lon)
            }
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

        model.onUpdatedDevice().observe(this) {
            onUpdatedDevice(it)
        }

        val adapter = SearchResultsAdapter {
            model.getLocationFromSearchSuggestion(it)
            hideKeyboard()
        }

        binding.addressSearchView.setAdapter(
            adapter,
            onTextChanged = { model.getSearchSuggestions(it) },
            onMyLocationClicked = {
                requestLocationPermissions(this) {
                    model.getLocation()
                }
            }
        )

        model.getCells()
    }

    private fun getMapFragment(): EditLocationMapFragment {
        return binding.mapView.getFragment()
    }

    private fun onUpdatedDevice(resource: Resource<UIDevice>) {
        binding.progress.visible(resource.status == Status.LOADING)
        binding.confirmBtn.isEnabled = resource.status == Status.ERROR

        if (resource.status == Status.SUCCESS) {
            binding.appBar.visible(false)
            binding.mainContainer.visible(false)
            binding.successView.htmlSubtitle(R.string.location_confirmed_desc)
            binding.dismissBtn.setOnClickListener {
                val resultValue = Intent().putExtra(ARG_DEVICE, resource.data)
                setResult(Activity.RESULT_OK, resultValue)
                finish()
            }
            binding.successContainer.visible(true)
        } else if (resource.status == Status.ERROR) {
            resource.message?.let { toast(it, Toast.LENGTH_LONG) }
        }
    }

    override fun onMapReady() {
        getMapFragment().addOnMapIdleListener { point ->
            point?.let {
                model.getAddressFromPoint(it)
                model.isPointOnBelowCapacityCell(it.latitude(), it.longitude())
            }
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

        model.onReverseGeocodedAddress().observe(this) {
            getMapFragment().showMarkerAddress(it)
        }

        model.onMoveToLocation().observe(this) {
            getMapFragment().moveToLocation(it)
            binding.addressSearchView.clear()
        }

        model.onCapacityLayer().observe(this) { layerData ->
            layerData?.let {
                getMapFragment().drawCapacityLayers(it)
            }
        }

        model.onCellWithBelowCapacity().observe(this) { isBelowCapacity ->
            if (isBelowCapacity == null || isBelowCapacity) {
                snackbar?.dismiss()
            } else {
                showSnackbarMessage(
                    viewGroup = binding.root,
                    message = getString(R.string.cell_already_max_capacity),
                    callback = {
                        navigator.openWebsite(this, getString(R.string.docs_url_cell_capacity))
                        snackbar?.dismiss()
                    },
                    actionTextResId = R.string.read_more,
                    anchorView = binding.bottomCard
                )
            }
        }

        getMapFragment().initMarkerAndListeners()
        getMapFragment().moveToLocation(device?.location)
    }
}
