package com.weatherxm.ui.claimdevice.wifi

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityClaimWifiDeviceBinding
import com.weatherxm.ui.claimdevice.location.ClaimLocationFragment
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiActivity.ClaimDevicePagerAdapter.Companion.PAGE_LOCATION
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiActivity.ClaimDevicePagerAdapter.Companion.PAGE_RESULT
import com.weatherxm.ui.claimdevice.wifi.connectwifi.ClaimWifiConnectWifiFragment
import com.weatherxm.ui.claimdevice.wifi.result.ClaimWifiResultFragment
import com.weatherxm.ui.claimdevice.wifi.verify.ClaimWifiVerifyFragment
import com.weatherxm.ui.claimdevice.wifi.verify.ClaimWifiVerifyViewModel
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.common.classSimpleName
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ClaimWifiActivity : BaseActivity() {
    companion object {
        const val CURRENT_PAGE = "current_page"
        const val SERIAL_NUMBER = "serial_number"
    }

    private lateinit var binding: ActivityClaimWifiDeviceBinding
    private val model: ClaimWifiViewModel by viewModel {
        parametersOf(intent.parcelable<DeviceType>(Contracts.ARG_DEVICE_TYPE))
    }
    private val locationModel: ClaimLocationViewModel by viewModel()
    private val verifyModel: ClaimWifiVerifyViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClaimWifiDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ClaimDevicePagerAdapter(this, model.deviceType)
        binding.pager.adapter = pagerAdapter
        binding.pager.isUserInputEnabled = false

        model.onCancel().observe(this) {
            if (it) finish()
        }

        model.onNext().observe(this) {
            if (it) onNextPressed()
        }

        binding.toolbar.title = if (model.deviceType == DeviceType.M5_WIFI) {
            getString(R.string.title_claim_m5_wifi)
        } else {
            getString(R.string.title_claim_d1_wifi)
        }
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
        updateClaimingProgress()
    }

    @Suppress("MagicNumber")
    private fun updateClaimingProgress() {
        binding.progress.progress = when (binding.pager.currentItem) {
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
            updateClaimingProgress()
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
        activity: AppCompatActivity,
        private val deviceType: DeviceType
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
                PAGE_CONNECT_WIFI -> ClaimWifiConnectWifiFragment()
                PAGE_SERIAL_NUMBER -> ClaimWifiVerifyFragment()
                PAGE_LOCATION -> ClaimLocationFragment.newInstance(deviceType)
                PAGE_RESULT -> ClaimWifiResultFragment()
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
