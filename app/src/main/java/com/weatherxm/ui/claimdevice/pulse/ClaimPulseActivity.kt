package com.weatherxm.ui.claimdevice.pulse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityClaimDeviceBinding
import com.weatherxm.service.GlobalUploadObserverService
import com.weatherxm.ui.claimdevice.beforeyouclaim.ClaimBeforeYouClaimFragment
import com.weatherxm.ui.claimdevice.location.ClaimLocationFragment
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.photosgallery.ClaimPhotosGalleryFragment
import com.weatherxm.ui.claimdevice.photosgallery.ClaimPhotosGalleryViewModel
import com.weatherxm.ui.claimdevice.photosintro.ClaimPhotosIntroFragment
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseActivity.ClaimPulseDevicePagerAdapter.Companion.PAGE_COUNT
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseActivity.ClaimPulseDevicePagerAdapter.Companion.PAGE_LOCATION
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseActivity.ClaimPulseDevicePagerAdapter.Companion.PAGE_PHOTOS_GALLERY
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseActivity.ClaimPulseDevicePagerAdapter.Companion.PAGE_RESULT
import com.weatherxm.ui.claimdevice.pulse.claimingcode.ClaimPulseClaimingCodeFragment
import com.weatherxm.ui.claimdevice.pulse.manualdetails.ClaimPulseManualDetailsFragment
import com.weatherxm.ui.claimdevice.pulse.preparegateway.ClaimPulsePrepareGatewayFragment
import com.weatherxm.ui.claimdevice.pulse.reboot.ClaimPulseRebootFragment
import com.weatherxm.ui.claimdevice.result.ClaimResultFragment
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.getValue

class ClaimPulseActivity : BaseActivity() {
    companion object {
        const val CURRENT_PAGE = "current_page"
        const val SERIAL_NUMBER = "serial_number"
        const val CLAIMING_KEY = "claiming_key"
    }

    private lateinit var binding: ActivityClaimDeviceBinding
    private val uploadObserverService: GlobalUploadObserverService by inject()
    private val model: ClaimPulseViewModel by viewModel()
    private val locationModel: ClaimLocationViewModel by viewModel()
    private val photosViewModel: ClaimPhotosGalleryViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClaimDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ClaimPulseDevicePagerAdapter(this)
        binding.pager.adapter = pagerAdapter
        binding.pager.isUserInputEnabled = false
        binding.progress.max = PAGE_COUNT - 1

        model.onCancel().observe(this) {
            if (it) finish()
        }

        model.onNext().observe(this) {
            if (it > 0) onNextPressed(it)
        }

        binding.toolbar.title = getString(R.string.title_claim_pulse_4g)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        model.onPhotosMetadata().observe(this) { (device, metadata) ->
            val numberOfPhotosToUpload = List(photosViewModel.onPhotos.size) { index ->
                metadata.getOrNull(index)
            }.filterNotNull().size
            uploadObserverService.setData(device, numberOfPhotosToUpload)

            startWorkerForUploadingPhotos(
                device,
                photosViewModel.onPhotos.toList(),
                metadata,
                device.id
            )
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
        analytics.trackScreen(AnalyticsService.Screen.CLAIM_PULSE, classSimpleName())
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
                    model.claimDevice(
                        locationModel.getInstallationLocation(),
                        photosViewModel.onPhotos
                    )
                }
            }
        }
    }

    private class ClaimPulseDevicePagerAdapter(
        activity: AppCompatActivity
    ) : FragmentStateAdapter(activity) {
        companion object {
            const val PAGE_BEFORE_CLAIMING = 0
            const val PAGE_REBOOT = 1
            const val PAGE_PREPARE_GATEWAY = 2
            const val PAGE_MANUAL_DETAILS = 3
            const val PAGE_CLAIMING_CODE = 4
            const val PAGE_LOCATION = 5
            const val PAGE_PHOTOS_INTRO = 6
            const val PAGE_PHOTOS_GALLERY = 7
            const val PAGE_RESULT = 8
            const val PAGE_COUNT = 9
        }

        override fun getItemCount(): Int = PAGE_COUNT

        @Suppress("UseCheckOrError", "UseIfInsteadOfWhen")
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                PAGE_BEFORE_CLAIMING -> ClaimBeforeYouClaimFragment.newInstance(DeviceType.PULSE_4G)
                PAGE_REBOOT -> ClaimPulseRebootFragment()
                PAGE_PREPARE_GATEWAY -> ClaimPulsePrepareGatewayFragment()
                PAGE_MANUAL_DETAILS -> ClaimPulseManualDetailsFragment()
                PAGE_CLAIMING_CODE -> ClaimPulseClaimingCodeFragment()
                PAGE_LOCATION -> ClaimLocationFragment.newInstance(DeviceType.PULSE_4G)
                PAGE_PHOTOS_INTRO -> ClaimPhotosIntroFragment.newInstance(DeviceType.PULSE_4G)
                PAGE_PHOTOS_GALLERY -> ClaimPhotosGalleryFragment.newInstance(DeviceType.PULSE_4G)
                PAGE_RESULT -> ClaimResultFragment.newInstance(DeviceType.PULSE_4G)
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
