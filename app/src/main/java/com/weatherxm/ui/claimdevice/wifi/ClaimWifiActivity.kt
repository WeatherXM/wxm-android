package com.weatherxm.ui.claimdevice.wifi

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityClaimDeviceBinding
import com.weatherxm.ui.claimdevice.beforeyouclaim.ClaimBeforeYouClaimFragment
import com.weatherxm.ui.claimdevice.location.ClaimLocationFragment
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.photosgallery.ClaimPhotosGalleryFragment
import com.weatherxm.ui.claimdevice.photosgallery.ClaimPhotosGalleryViewModel
import com.weatherxm.ui.claimdevice.photosintro.ClaimPhotosIntroFragment
import com.weatherxm.ui.claimdevice.result.ClaimResultFragment
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiActivity.ClaimDevicePagerAdapter.Companion.PAGE_COUNT
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiActivity.ClaimDevicePagerAdapter.Companion.PAGE_LOCATION
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiActivity.ClaimDevicePagerAdapter.Companion.PAGE_PHOTOS_GALLERY
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiActivity.ClaimDevicePagerAdapter.Companion.PAGE_RESULT
import com.weatherxm.ui.claimdevice.wifi.connectwifi.ClaimWifiConnectWifiFragment
import com.weatherxm.ui.claimdevice.wifi.manualdetails.ClaimWifiManualDetailsFragment
import com.weatherxm.ui.claimdevice.wifi.preparegateway.ClaimWifiPrepareGatewayFragment
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ClaimWifiActivity : BaseActivity() {
    companion object {
        const val CURRENT_PAGE = "current_page"
        const val SERIAL_NUMBER = "serial_number"
        const val CLAIMING_KEY = "claiming_key"
    }

    private lateinit var binding: ActivityClaimDeviceBinding
    private val model: ClaimWifiViewModel by viewModel {
        parametersOf(intent.parcelable<DeviceType>(Contracts.ARG_DEVICE_TYPE))
    }
    private val locationModel: ClaimLocationViewModel by viewModel()
    private val photosViewModel: ClaimPhotosGalleryViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClaimDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ClaimDevicePagerAdapter(this, model.deviceType)
        binding.pager.adapter = pagerAdapter
        binding.pager.isUserInputEnabled = false
        binding.progress.max = PAGE_COUNT - 1

        model.onCancel().observe(this) {
            if (it) finish()
        }

        model.onNext().observe(this) {
            if (it > 0) onNextPressed(it)
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
            binding.progress.progress = binding.pager.currentItem + 1
            val savedSn = it.getString(SERIAL_NUMBER, String.empty())
            if (model.validateSerial(savedSn)) {
                model.setSerialNumber(savedSn)
            }
            val savedKey = it.getString(CLAIMING_KEY, String.empty())
            if (model.validateClaimingKey(savedKey)) {
                model.setClaimingKey(savedKey)
            }
        }
        binding.progress.progress = binding.pager.currentItem + 1
    }

    override fun onResume() {
        super.onResume()
        if (model.deviceType == DeviceType.M5_WIFI) {
            analytics.trackScreen(AnalyticsService.Screen.CLAIM_M5, classSimpleName())
        } else {
            analytics.trackScreen(AnalyticsService.Screen.CLAIM_D1, classSimpleName())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_PAGE, binding.pager.currentItem)
        outState.putString(SERIAL_NUMBER, model.getSerialNumber())
        outState.putString(CLAIMING_KEY, model.getClaimingKey())
        super.onSaveInstanceState(outState)
    }

    private fun onNextPressed(incrementPage: Int) {
        with(binding) {
            pager.currentItem += incrementPage
            binding.progress.progress = pager.currentItem + 1
            when (pager.currentItem) {
                PAGE_LOCATION -> locationModel.requestUserLocation()
                PAGE_PHOTOS_GALLERY -> photosViewModel.requestCameraPermission()
                PAGE_RESULT -> {
                    binding.appBar.visible(false)
                    binding.progress.visible(false)
                    model.claimDevice(locationModel.getInstallationLocation())
                }
            }
        }
    }

    private class ClaimDevicePagerAdapter(
        activity: AppCompatActivity,
        private val deviceType: DeviceType
    ) : FragmentStateAdapter(activity) {
        companion object {
            const val PAGE_BEFORE_CLAIMING = 0
            const val PAGE_CONNECT_WIFI = 1
            const val PAGE_PREPARE_GATEWAY = 2
            const val PAGE_MANUAL_DETAILS = 3
            const val PAGE_LOCATION = 4
            const val PAGE_PHOTOS_INTRO = 5
            const val PAGE_PHOTOS_GALLERY = 6
            const val PAGE_RESULT = 7
            const val PAGE_COUNT = 8
        }

        override fun getItemCount(): Int = PAGE_COUNT

        @Suppress("UseCheckOrError")
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                PAGE_BEFORE_CLAIMING -> ClaimBeforeYouClaimFragment.newInstance(deviceType)
                PAGE_CONNECT_WIFI -> ClaimWifiConnectWifiFragment()
                PAGE_PREPARE_GATEWAY -> ClaimWifiPrepareGatewayFragment()
                PAGE_MANUAL_DETAILS -> ClaimWifiManualDetailsFragment()
                PAGE_LOCATION -> ClaimLocationFragment.newInstance(deviceType)
                PAGE_PHOTOS_INTRO -> ClaimPhotosIntroFragment.newInstance(deviceType)
                PAGE_PHOTOS_GALLERY -> ClaimPhotosGalleryFragment.newInstance(deviceType)
                PAGE_RESULT -> ClaimResultFragment.newInstance(deviceType)
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
