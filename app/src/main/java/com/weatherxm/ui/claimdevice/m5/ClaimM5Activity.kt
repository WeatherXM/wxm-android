package com.weatherxm.ui.claimdevice.m5

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.weatherxm.R
import com.weatherxm.databinding.ActivityClaimM5DeviceBinding
import com.weatherxm.ui.claimdevice.location.ClaimLocationFragment
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.m5.ClaimM5Activity.ClaimDevicePagerAdapter.Companion.PAGE_LOCATION
import com.weatherxm.ui.claimdevice.m5.ClaimM5Activity.ClaimDevicePagerAdapter.Companion.PAGE_RESULT
import com.weatherxm.ui.claimdevice.m5.ClaimM5Activity.ClaimDevicePagerAdapter.Companion.PAGE_SERIAL_NUMBER
import com.weatherxm.ui.claimdevice.m5.information.ClaimM5InformationFragment
import com.weatherxm.ui.claimdevice.m5.verify.ClaimM5VerifyFragment
import com.weatherxm.ui.claimdevice.m5.verify.ClaimM5VerifyViewModel
import com.weatherxm.ui.claimdevice.m5.result.ClaimM5ResultFragment
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.util.applyInsets
import com.weatherxm.util.setIcon
import com.weatherxm.util.setSuccessChip

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

        model.onCancel().observe(this) {
            if (it) finish()
        }

        model.onNext().observe(this) {
            if (it) onNextPressed()
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        onBackPressedDispatcher.addCallback {
            finish()
        }

        model.fetchUserEmail()

        savedInstanceState?.let {
            binding.pager.currentItem = it.getInt(CURRENT_PAGE, 0)
            val savedSn = it.getString(SERIAL_NUMBER, "")
            if (verifyModel.validateSerial(savedSn)) {
                verifyModel.setSerialNumber(savedSn)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_PAGE, binding.pager.currentItem)
        outState.putString(SERIAL_NUMBER, verifyModel.getSerialNumber())
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("MissingPermission")
    private fun onNextPressed() {
        with(binding) {
            pager.currentItem += 1

            when (pager.currentItem) {
                PAGE_SERIAL_NUMBER -> {
                    connection.setSuccessChip()
                    verify.setIcon(R.drawable.ic_two_filled)
                }
                PAGE_LOCATION -> {
                    verify.setSuccessChip()
                    location.setIcon(R.drawable.ic_three_filled)
                    locationModel.requestUserLocation()
                }
                PAGE_RESULT -> {
                    model.claimDevice(
                        verifyModel.getSerialNumber(),
                        locationModel.getInstallationLocation()
                    )
                    location.setSuccessChip()
                }
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
                PAGE_RESULT -> ClaimM5ResultFragment()
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
