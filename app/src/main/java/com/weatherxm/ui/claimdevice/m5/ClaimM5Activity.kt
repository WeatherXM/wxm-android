package com.weatherxm.ui.claimdevice.m5

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityClaimM5DeviceBinding
import com.weatherxm.ui.claimdevice.location.ClaimLocationFragment
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.m5.ClaimM5Activity.ClaimDevicePagerAdapter.Companion.PAGE_LOCATION
import com.weatherxm.ui.claimdevice.m5.ClaimM5Activity.ClaimDevicePagerAdapter.Companion.PAGE_RESULT
import com.weatherxm.ui.claimdevice.m5.connectwifi.ClaimM5ConnectWifiFragment
import com.weatherxm.ui.claimdevice.m5.result.ClaimM5ResultFragment
import com.weatherxm.ui.claimdevice.m5.verify.ClaimM5VerifyFragment
import com.weatherxm.ui.claimdevice.m5.verify.ClaimM5VerifyViewModel
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.common.classSimpleName
import org.koin.androidx.viewmodel.ext.android.viewModel

class ClaimM5Activity : BaseActivity() {
    companion object {
        const val CURRENT_PAGE = "current_page"
        const val SERIAL_NUMBER = "serial_number"
    }

    private lateinit var binding: ActivityClaimM5DeviceBinding
    private val model: ClaimM5ViewModel by viewModel()
    private val locationModel: ClaimLocationViewModel by viewModel()
    private val verifyModel: ClaimM5VerifyViewModel by viewModel()

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

        binding.toolbar.title = getString(R.string.title_claim_m5_wifi)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        savedInstanceState?.let {
            binding.pager.currentItem = it.getInt(CURRENT_PAGE, 0)
            val savedSn = it.getString(SERIAL_NUMBER, String.empty())
            if (verifyModel.validateSerial(savedSn)) {
                verifyModel.setSerialNumber(savedSn)
            }
        }
        setClaimingProgress(binding.pager.currentItem)
    }

    private fun setClaimingProgress(currentPage: Int) {
        binding.progress.progress = when (currentPage) {
            0 -> 10
            1 -> 30
            2 -> 100
            else -> 100
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.CLAIM_M5, classSimpleName())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_PAGE, binding.pager.currentItem)
        outState.putString(SERIAL_NUMBER, verifyModel.getSerialNumber())
        super.onSaveInstanceState(outState)
    }

    private fun onNextPressed() {
        with(binding) {
            pager.currentItem += 1
            setClaimingProgress(pager.currentItem)
            when (pager.currentItem) {
                PAGE_LOCATION -> {
                    locationModel.requestUserLocation()
                }
                PAGE_RESULT -> {
                    binding.appBar.setVisible(false)
                    binding.progress.setVisible(false)
                    model.claimDevice(
                        verifyModel.getSerialNumber(),
                        locationModel.getInstallationLocation()
                    )
                }
            }
        }
    }

    private class ClaimDevicePagerAdapter(
        activity: AppCompatActivity
    ) : FragmentStateAdapter(activity) {
        companion object {
            const val PAGE_CONNECT_WIFI = 0
            const val PAGE_SERIAL_NUMBER = 1
            const val PAGE_LOCATION = 2
            const val PAGE_RESULT = 3
            const val PAGE_COUNT = 4
        }

        override fun getItemCount(): Int = PAGE_COUNT

        @Suppress("UseCheckOrError")
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                PAGE_CONNECT_WIFI -> ClaimM5ConnectWifiFragment()
                PAGE_SERIAL_NUMBER -> ClaimM5VerifyFragment()
                PAGE_LOCATION -> ClaimLocationFragment.newInstance(DeviceType.M5_WIFI)
                PAGE_RESULT -> ClaimM5ResultFragment()
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
