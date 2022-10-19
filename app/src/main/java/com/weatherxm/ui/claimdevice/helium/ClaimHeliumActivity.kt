package com.weatherxm.ui.claimdevice.helium

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.weatherxm.R
import com.weatherxm.databinding.ActivityClaimHeliumDeviceBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumActivity.ClaimHeliumDevicePagerAdapter.Companion.PAGE_LOCATION
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumActivity.ClaimHeliumDevicePagerAdapter.Companion.PAGE_RESULT
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumActivity.ClaimHeliumDevicePagerAdapter.Companion.PAGE_VERIFY
import com.weatherxm.ui.claimdevice.helium.reset.ClaimHeliumResetFragment
import com.weatherxm.ui.claimdevice.helium.verify.ClaimHeliumVerifyFragment
import com.weatherxm.ui.claimdevice.helium.verify.ClaimHeliumVerifyViewModel
import com.weatherxm.ui.claimdevice.location.ClaimLocationFragment
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.result.ClaimResultFragment
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.common.toast
import com.weatherxm.util.applyInsets
import com.weatherxm.util.setIcon
import com.weatherxm.util.setIconAndColor
import timber.log.Timber

class ClaimHeliumActivity : AppCompatActivity() {
    private val model: ClaimHeliumViewModel by viewModels()
    private val locationModel: ClaimLocationViewModel by viewModels()
    private val verifyModel: ClaimHeliumVerifyViewModel by viewModels()
    private lateinit var binding: ActivityClaimHeliumDeviceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClaimHeliumDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ClaimHeliumDevicePagerAdapter(this)
        binding.pager.adapter = pagerAdapter
        binding.pager.isUserInputEnabled = false

        onBackPressedDispatcher.addCallback {
            finish()
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        model.onCancel().observe(this) {
            if (it) finish()
        }

        model.onNext().observe(this) {
            if (it) onNextPressed()
        }

        model.fetchUserEmail()
    }

    @SuppressLint("MissingPermission")
    private fun onNextPressed() {
        with(binding) {
            pager.currentItem += 1

            when (pager.currentItem) {
                PAGE_VERIFY -> {
                    reset.setIconAndColor(R.drawable.ic_checkmark, R.color.success_tint)
                    reset.setChipIconTintResource(R.color.dark_background)
                    reset.setTextColor(getColor(R.color.dark_background))
                    verify.setIcon(R.drawable.ic_two_filled)
                }
                PAGE_LOCATION -> {
                    verify.setIconAndColor(R.drawable.ic_checkmark, R.color.success_tint)
                    verify.setChipIconTintResource(R.color.dark_background)
                    verify.setTextColor(getColor(R.color.dark_background))
                    location.setIcon(R.drawable.ic_three_filled)

                    checkPermissionsAndThen(
                        permissions = arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        rationaleTitle = getString(R.string.permission_location_title),
                        rationaleMessage = getString(R.string.permission_location_rationale),
                        onGranted = {
                            // Get last location
                            locationModel.getLocationAndThen(this@ClaimHeliumActivity) { location ->
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
                PAGE_RESULT -> {
                    model.claimDevice(
                        verifyModel.getDevEUI(),
                        verifyModel.getDeviceKey(),
                        locationModel.getInstallationLocation()
                    )
                    location.setIconAndColor(R.drawable.ic_checkmark, R.color.success_tint)
                    location.setChipIconTintResource(R.color.dark_background)
                    location.setTextColor(getColor(R.color.dark_background))
                }
            }
        }
    }

    private class ClaimHeliumDevicePagerAdapter(
        activity: AppCompatActivity
    ) : FragmentStateAdapter(activity) {
        companion object {
            const val PAGE_RESET = 0
            const val PAGE_VERIFY = 1
            const val PAGE_LOCATION = 2
            const val PAGE_RESULT = 3
            const val PAGE_COUNT = 4
        }

        override fun getItemCount(): Int = PAGE_COUNT

        @Suppress("UseCheckOrError")
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                PAGE_RESET -> ClaimHeliumResetFragment()
                PAGE_VERIFY -> ClaimHeliumVerifyFragment()
                PAGE_LOCATION -> ClaimLocationFragment.newInstance(DeviceType.HELIUM)
                PAGE_RESULT -> ClaimResultFragment()
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
