package com.weatherxm.ui.claimdevice

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.weatherxm.R
import com.weatherxm.databinding.ActivityClaimDeviceBinding
import com.weatherxm.ui.claimdevice.ClaimDeviceActivity.ClaimDevicePagerAdapter.Companion.PAGE_INFORMATION
import com.weatherxm.ui.claimdevice.ClaimDeviceActivity.ClaimDevicePagerAdapter.Companion.PAGE_RESULT
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.common.hasPermission
import com.weatherxm.ui.common.toast
import com.weatherxm.util.applyInsets
import org.koin.core.component.KoinComponent
import timber.log.Timber

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
                checkPermissionsAndThen(
                    permissions = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                    rationaleTitle = getString(R.string.permission_location_title),
                    rationaleMessage = getString(R.string.permission_location_rationale),
                    /*
                    * onGranted runs only when ACCESS_FINE_LOCATION has been given
                    * (as this is the first permission asked)
                     */
                    onGranted = {
                        // Get last location
                        model.getLocationAndThen(this) { location ->
                            Timber.d("Got user location: $location")
                            if(location == null) {
                                toast(R.string.gps_location_failed)
                            } else {
                                model.updateLocationOnMap(location)
                            }
                        }
                    },
                    /*
                    * onDenied runs when
                    * 1. ACCESS_COARSE_LOCATION has been given and ACCESS_FINE_LOCATION has not
                    * 2. none of them has been given
                     */
                    onDenied = {
                        if(hasPermission(ACCESS_COARSE_LOCATION)) {
                            // Get last location
                            model.getLocationAndThen(this) { location ->
                                Timber.d("Got user location: $location")
                                if(location == null) {
                                    toast(R.string.gps_location_failed)
                                } else {
                                    model.updateLocationOnMap(location)
                                }
                            }
                        }
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
