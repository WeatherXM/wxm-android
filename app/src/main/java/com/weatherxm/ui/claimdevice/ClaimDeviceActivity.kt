package com.weatherxm.ui.claimdevice

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.weatherxm.R
import com.weatherxm.databinding.ActivityClaimDeviceBinding
import com.weatherxm.ui.claimdevice.ClaimDeviceActivity.ClaimDevicePagerAdapter.Companion.PAGE_INFORMATION
import com.weatherxm.ui.claimdevice.ClaimDeviceActivity.ClaimDevicePagerAdapter.Companion.PAGE_RESULT
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.common.toast
import com.weatherxm.util.applyInsets
import org.koin.core.component.KoinComponent
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ClaimDeviceActivity : FragmentActivity(), KoinComponent {

    private val model: ClaimDeviceViewModel by viewModels()
    private lateinit var binding: ActivityClaimDeviceBinding

    private lateinit var locationClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClaimDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ClaimDevicePagerAdapter(this)
        binding.pager.adapter = pagerAdapter
        binding.pager.isUserInputEnabled = false

        model.onStep().observe(this) { step ->
            if (step < 0) {
                onBackPressed()
            } else if (step > 0) {
                binding.pager.currentItem += 1
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        model.onGPS().observe(this) { useGPS ->
            if (useGPS) {
                locationClient = LocationServices.getFusedLocationProviderClient(this)

                checkPermissionsAndThen(
                    permissions = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                    rationaleTitle = getString(R.string.permission_location_title),
                    rationaleMessage = getString(R.string.permission_location_rationale),
                    onGranted = {
                        // Get last location
                        getLocationAndThen { location ->
                            Timber.d("Got user location: $location")
                            model.updateLocationOnMap(location)
                        }
                    },
                    onDenied = {
                        // TODO Check if we have at least coarse location permission
                    }
                )
            }
        }

        model.fetchUserEmail()
    }

    override fun onBackPressed() {
        when (binding.pager.currentItem) {
            PAGE_INFORMATION, PAGE_RESULT -> {
                // If the user is currently looking at the first
                // or the final step (although only on failure on the final step this code runs)
                // go back to home and finish this activity
                super.onBackPressed()
                finish()
            }
            else -> {
                // Otherwise, select the previous step.
                binding.pager.currentItem = binding.pager.currentItem - 1
            }
        }
    }

    // TODO: Check whether we can move this code to view model
    @Suppress("MagicNumber")
    @RequiresPermission(anyOf = [ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION])
    private fun getLocationAndThen(onLocation: (location: Location) -> Unit) {
        val priority = when (PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) -> {
                PRIORITY_HIGH_ACCURACY
            }
            ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) -> {
                PRIORITY_BALANCED_POWER_ACCURACY
            }
            else -> {
                null
            }
        }
        priority?.let { it ->
            locationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location == null) {
                        Timber.d("Current location is null. Requesting fresh location.")
                        locationClient.requestLocationUpdates(
                            LocationRequest.create()
                                .setNumUpdates(1)
                                .setInterval(TimeUnit.SECONDS.toMillis(2))
                                .setFastestInterval(0)
                                .setMaxWaitTime(TimeUnit.SECONDS.toMillis(3))
                                .setPriority(it),
                            object : LocationCallback() {
                                override fun onLocationResult(result: LocationResult) {
                                    onLocation.invoke(result.lastLocation)
                                }
                            },
                            Looper.getMainLooper()
                        )
                    } else {
                        Timber.d("Got current location: $location")
                        onLocation.invoke(location)
                    }
                }
                .addOnFailureListener {
                    Timber.d(it, "Could not get current location.")
                    toast("Could not get current location.")
                }
        }
    }

    private class ClaimDevicePagerAdapter(
        activity: FragmentActivity
    ) : FragmentStateAdapter(activity) {
        companion object {
            const val PAGE_INFORMATION = 0
            const val PAGE_SERIAL_NUMBER = 1
            const val PAGE_LOCATION = 2
            const val PAGE_RESULT = 3
            const val PAGE_COUNT = 4
        }

        override fun getItemCount(): Int = PAGE_COUNT

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                PAGE_INFORMATION -> ClaimDeviceInformationFragment()
                PAGE_SERIAL_NUMBER -> ClaimDeviceSerialNumberFragment()
                PAGE_LOCATION -> ClaimDeviceLocationFragment()
                PAGE_RESULT -> ClaimDeviceResultFragment()
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
