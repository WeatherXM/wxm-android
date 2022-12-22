package com.weatherxm.ui.claimdevice.helium

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.weatherxm.R
import com.weatherxm.databinding.ActivityClaimHeliumDeviceBinding
import com.weatherxm.ui.claimdevice.helium.frequency.ClaimHeliumFrequencyFragment
import com.weatherxm.ui.claimdevice.helium.frequency.ClaimHeliumFrequencyViewModel
import com.weatherxm.ui.claimdevice.helium.pair.ClaimHeliumPairFragment
import com.weatherxm.ui.claimdevice.helium.pair.ClaimHeliumPairViewModel
import com.weatherxm.ui.claimdevice.helium.reset.ClaimHeliumResetFragment
import com.weatherxm.ui.claimdevice.location.ClaimLocationFragment
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.result.ClaimResultFragment
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.checkPermissionsAndThen
import com.weatherxm.ui.common.toast
import com.weatherxm.util.applyInsets
import com.weatherxm.util.setIcon
import com.weatherxm.util.setSuccessChip
import timber.log.Timber

class ClaimHeliumActivity : AppCompatActivity() {
    companion object {
        const val CURRENT_PAGE = "current_page"
        const val DEV_EUI = "dev_eui"
        const val DEV_KEY = "dev_key"
    }

    private val model: ClaimHeliumViewModel by viewModels()
    private val locationModel: ClaimLocationViewModel by viewModels()
    private val frequencyModel: ClaimHeliumFrequencyViewModel by viewModels()
    private val pairModel: ClaimHeliumPairViewModel by viewModels()
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

        model.onBackToLocation().observe(this) {
            if(it) {
                binding.pager.currentItem -= 1
                binding.thirdStep.setIcon(R.drawable.ic_three_filled)
                binding.fourthStep.setIcon(R.drawable.ic_four_outlined)
            }
        }

        locationModel.onGetUserLocation().observe(this) {
            if (it) requestLocationPermissions()
        }

        model.fetchUserEmail()

        savedInstanceState?.let {
            binding.pager.currentItem = it.getInt(CURRENT_PAGE, 0)
            model.setDeviceEUI(it.getString(DEV_EUI, ""))
            model.setDeviceKey(it.getString(DEV_KEY, ""))
        }
    }

    override fun onDestroy() {
        pairModel.disconnectFromPeripheral()
        super.onDestroy()
    }

    private fun onNextPressed() {
        with(binding) {
            pager.currentItem += 1

            when (pager.currentItem) {
                ClaimHeliumDevicePagerAdapter.PAGE_VERIFY_OR_PAIR -> {
                    firstStep.setSuccessChip()
                    secondStep.setIcon(R.drawable.ic_two_filled)
                }
                ClaimHeliumDevicePagerAdapter.PAGE_LOCATION -> {
                    secondStep.setSuccessChip()
                    thirdStep.setIcon(R.drawable.ic_three_filled)
                    requestLocationPermissions()
                }
                ClaimHeliumDevicePagerAdapter.PAGE_FREQUENCY -> {
                    thirdStep.setSuccessChip()
                    fourthStep.setIcon(R.drawable.ic_four_filled)
                    frequencyModel.getCountryAndFrequencies(locationModel.getInstallationLocation())
                }
                ClaimHeliumDevicePagerAdapter.PAGE_RESULT -> {
                    model.claimDevice(locationModel.getInstallationLocation())
                    fourthStep.setSuccessChip()
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        checkPermissionsAndThen(
            permissions = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
            rationaleTitle = getString(R.string.permission_location_title),
            rationaleMessage = getString(R.string.permission_location_rationale),
            onGranted = { getUserLocation() },
            onDenied = { toast(R.string.error_claim_gps_failed) }
        )
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        // Get last location
        locationModel.getLocationAndThen(this@ClaimHeliumActivity) {
            Timber.d("Got user location: $it")
            if (it == null) {
                toast(R.string.error_claim_gps_failed)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_PAGE, binding.pager.currentItem)
        outState.putString(DEV_EUI, model.getDevEUI())
        outState.putString(DEV_KEY, model.getDeviceKey())
        super.onSaveInstanceState(outState)
    }

    private class ClaimHeliumDevicePagerAdapter(
        activity: AppCompatActivity,
    ) : FragmentStateAdapter(activity) {
        companion object {
            const val PAGE_RESET = 0
            const val PAGE_VERIFY_OR_PAIR = 1
            const val PAGE_LOCATION = 2
            const val PAGE_FREQUENCY = 3
            const val PAGE_RESULT = 4
            const val PAGE_COUNT = 5
        }

        override fun getItemCount(): Int = PAGE_COUNT

        @Suppress("UseCheckOrError")
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                PAGE_RESET -> ClaimHeliumResetFragment()
                PAGE_VERIFY_OR_PAIR -> ClaimHeliumPairFragment()
                PAGE_LOCATION -> ClaimLocationFragment.newInstance(DeviceType.HELIUM)
                PAGE_FREQUENCY -> ClaimHeliumFrequencyFragment()
                PAGE_RESULT -> ClaimResultFragment()
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
