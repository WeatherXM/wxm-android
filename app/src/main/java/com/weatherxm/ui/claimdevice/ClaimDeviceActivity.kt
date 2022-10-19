package com.weatherxm.ui.claimdevice

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
import com.weatherxm.ui.claimdevice.ClaimDeviceActivity.ClaimDevicePagerAdapter.Companion.PAGE_INFORMATION
import com.weatherxm.ui.claimdevice.ClaimDeviceActivity.ClaimDevicePagerAdapter.Companion.PAGE_LOCATION
import com.weatherxm.ui.claimdevice.ClaimDeviceActivity.ClaimDevicePagerAdapter.Companion.PAGE_RESULT
import com.weatherxm.ui.claimdevice.ClaimDeviceActivity.ClaimDevicePagerAdapter.Companion.PAGE_SERIAL_NUMBER
import com.weatherxm.ui.claimdevice.location.ClaimDeviceLocationFragment
import com.weatherxm.ui.claimdevice.location.ClaimDeviceLocationViewModel
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.common.toast
import com.weatherxm.util.applyInsets
import timber.log.Timber

class ClaimDeviceActivity : AppCompatActivity() {
    companion object {
        const val CURRENT_PAGE = "current_page"
        const val SERIAL_NUMBER = "serial_number"
    }

    private val model: ClaimDeviceViewModel by viewModels()
    private lateinit var binding: ActivityClaimM5DeviceBinding
    private val locationModel: ClaimDeviceLocationViewModel by viewModels()
    private lateinit var binding: ActivityClaimDeviceBinding

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

        model.onNextButtonClick().observe(this) { shouldClick ->
            if (shouldClick) binding.nextBtn.performClick()
        }

        binding.nextBtn.setOnClickListener {
            if (binding.pager.currentItem == PAGE_SERIAL_NUMBER && !model.isSerialSet()) {
                model.checkSerialAndContinue()
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
            model.validateAndSetSerial(it.getString(SERIAL_NUMBER, ""))
            updateUI()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_PAGE, binding.pager.currentItem)
        outState.putString(SERIAL_NUMBER, model.getSerialNumber())
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("MissingPermission")
    private fun onNextPressed() {
        binding.pager.currentItem += 1

        if (binding.pager.currentItem == PAGE_RESULT) {
            model.claimDevice()
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
                PAGE_INFORMATION -> ClaimDeviceInformationFragment()
                PAGE_SERIAL_NUMBER -> ClaimDeviceSerialNumberFragment()
                PAGE_LOCATION -> ClaimDeviceLocationFragment()
                PAGE_RESULT -> ClaimDeviceResultFragment()
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
