package com.weatherxm.ui.claimdevice.m5

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.weatherxm.R
import com.weatherxm.databinding.ActivityClaimM5DeviceBinding
import com.weatherxm.ui.claimdevice.location.ClaimLocationFragment
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.m5.ClaimM5Activity.ClaimDevicePagerAdapter.Companion.PAGE_INFORMATION
import com.weatherxm.ui.claimdevice.m5.ClaimM5Activity.ClaimDevicePagerAdapter.Companion.PAGE_LOCATION
import com.weatherxm.ui.claimdevice.m5.ClaimM5Activity.ClaimDevicePagerAdapter.Companion.PAGE_RESULT
import com.weatherxm.ui.claimdevice.m5.ClaimM5Activity.ClaimDevicePagerAdapter.Companion.PAGE_SERIAL_NUMBER
import com.weatherxm.ui.claimdevice.m5.information.ClaimM5InformationFragment
import com.weatherxm.ui.claimdevice.m5.verify.ClaimM5VerifyFragment
import com.weatherxm.ui.claimdevice.m5.verify.ClaimM5VerifyViewModel
import com.weatherxm.ui.claimdevice.result.ClaimResultFragment
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.common.toast
import com.weatherxm.util.applyInsets
import timber.log.Timber

class ClaimM5Activity : AppCompatActivity() {
    companion object {
        const val CURRENT_PAGE = "current_page"
        const val SERIAL_NUMBER = "serial_number"
    }

    private lateinit var binding: ActivityClaimM5DeviceBinding
    private val model: ClaimM5ViewModel by viewModels()
    private val locationModel: ClaimLocationViewModel by viewModels()
    private val verifyModel: ClaimM5VerifyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClaimM5DeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ClaimDevicePagerAdapter(this)
        binding.pager.adapter = pagerAdapter
        binding.pager.isUserInputEnabled = false

        val pagerIndicator = binding.pagerIndicator
        pagerIndicator.attachTo(binding.pager)

        model.onCancel().observe(this) {
            if (it) finish()
        }

        model.onNextButtonEnabledStatus().observe(this) { enabled ->
            binding.nextBtn.isEnabled = enabled
        }

        model.onNext().observe(this) { shouldClick ->
            if (shouldClick) binding.nextBtn.performClick()
        }

        binding.nextBtn.setOnClickListener {
            if (binding.pager.currentItem == PAGE_SERIAL_NUMBER
                && verifyModel.getSerialNumber().isEmpty()
            ) {
                verifyModel.checkSerialAndContinue()
            } else if (binding.pager.currentItem == PAGE_LOCATION
                && !locationModel.isInstallationLocationValid()
            ) {
                locationModel.confirmLocation()
            } else {
                onNextPressed()
            }
        }

        // Make dots not clickable so the user can navigate only under our conditions
        binding.pagerIndicator.dotsClickable = false

        binding.prevBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }

        onBackPressedDispatcher.addCallback {
            when (binding.pager.currentItem) {
                PAGE_INFORMATION, PAGE_RESULT -> {
                    // If the user is currently looking at the first
                    // or the final step (although only on failure on the final step this code runs)
                    // go back to home and finish this activity
                    finish()
                }
                else -> {
                    // Otherwise, select the previous step.
                    binding.pager.currentItem = binding.pager.currentItem - 1
                }
            }
            updateUI()
        }

        model.fetchUserEmail()

        savedInstanceState?.let {
            binding.pager.currentItem = it.getInt(CURRENT_PAGE, 0)
            val savedSn = it.getString(SERIAL_NUMBER, "")
            if (verifyModel.validateSerial(savedSn)) {
                verifyModel.setSerialNumber(savedSn)
            }
            updateUI()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_PAGE, binding.pager.currentItem)
        outState.putString(SERIAL_NUMBER, verifyModel.getSerialNumber())
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("MissingPermission")
    private fun onNextPressed() {
        binding.pager.currentItem += 1

        if (binding.pager.currentItem == PAGE_RESULT) {
            model.claimDevice(
                verifyModel.getSerialNumber(),
                locationModel.getInstallationLocation()
            )
        }

        if (binding.pager.currentItem == PAGE_LOCATION) {
            checkPermissionsAndThen(
                permissions = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                rationaleTitle = getString(R.string.permission_location_title),
                rationaleMessage = getString(R.string.permission_location_rationale),
                onGranted = {
                    // Get last location
                    locationModel.getLocationAndThen(this) { location ->
                        Timber.d("Got user location: $location")
                        if (location == null) {
                            toast(R.string.error_claim_gps_failed)
                        }
                    }
                },
                onDenied = {
                    toast(R.string.error_claim_gps_failed)
                }
            )
        }
        updateUI()
    }

    private fun updateUI() {
        when (binding.pager.currentItem) {
            PAGE_INFORMATION -> {
                binding.prevBtn.visibility = View.INVISIBLE
                binding.nextBtn.isEnabled = true
            }
            PAGE_SERIAL_NUMBER -> {
                binding.prevBtn.visibility = View.VISIBLE
                binding.nextBtn.text = getString(R.string.action_next)
            }
            PAGE_LOCATION -> {
                binding.prevBtn.visibility = View.VISIBLE
                binding.nextBtn.text = getString(R.string.action_claim)
            }
            PAGE_RESULT -> {
                binding.prevBtn.visibility = View.GONE
                binding.pagerIndicator.visibility = View.GONE
                binding.nextBtn.visibility = View.GONE
            }
        }
    }

    private class ClaimDevicePagerAdapter(
        activity: AppCompatActivity
    ) : FragmentStateAdapter(activity) {
        companion object {
            const val PAGE_INFORMATION = 0
            const val PAGE_SERIAL_NUMBER = 1
            const val PAGE_LOCATION = 2
            const val PAGE_RESULT = 3
            const val PAGE_COUNT = 4
        }

        override fun getItemCount(): Int = PAGE_COUNT

        @Suppress("UseCheckOrError")
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                PAGE_INFORMATION -> ClaimM5InformationFragment()
                PAGE_SERIAL_NUMBER -> ClaimM5VerifyFragment()
                PAGE_LOCATION -> ClaimLocationFragment.newInstance(DeviceType.M5_WIFI)
                PAGE_RESULT -> ClaimResultFragment()
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
