package com.weatherxm.ui.claimdevice.helium

import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityClaimDeviceBinding
import com.weatherxm.service.GlobalUploadObserverService
import com.weatherxm.ui.claimdevice.beforeyouclaim.ClaimBeforeYouClaimFragment
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumActivity.ClaimHeliumDevicePagerAdapter.Companion.PAGE_COUNT
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumActivity.ClaimHeliumDevicePagerAdapter.Companion.PAGE_FREQUENCY
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumActivity.ClaimHeliumDevicePagerAdapter.Companion.PAGE_LOCATION
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumActivity.ClaimHeliumDevicePagerAdapter.Companion.PAGE_PHOTOS_GALLERY
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumActivity.ClaimHeliumDevicePagerAdapter.Companion.PAGE_RESULT
import com.weatherxm.ui.claimdevice.helium.frequency.ClaimHeliumFrequencyFragment
import com.weatherxm.ui.claimdevice.helium.frequency.ClaimHeliumFrequencyViewModel
import com.weatherxm.ui.claimdevice.helium.pair.ClaimHeliumPairFragment
import com.weatherxm.ui.claimdevice.helium.pair.ClaimHeliumPairViewModel
import com.weatherxm.ui.claimdevice.helium.reset.ClaimHeliumResetFragment
import com.weatherxm.ui.claimdevice.helium.result.ClaimHeliumResultFragment
import com.weatherxm.ui.claimdevice.helium.result.ClaimHeliumResultViewModel
import com.weatherxm.ui.claimdevice.location.ClaimLocationFragment
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.photosgallery.ClaimPhotosGalleryFragment
import com.weatherxm.ui.claimdevice.photosgallery.ClaimPhotosGalleryViewModel
import com.weatherxm.ui.claimdevice.photosintro.ClaimPhotosIntroFragment
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.getValue

class ClaimHeliumActivity : BaseActivity() {
    companion object {
        const val CURRENT_PAGE = "current_page"
        const val DEV_EUI = "dev_eui"
        const val DEV_KEY = "dev_key"
        const val CLAIMED_DEVICE = "claimed_device"
    }

    private val uploadObserverService: GlobalUploadObserverService by inject()
    private val model: ClaimHeliumViewModel by viewModel()
    private val locationModel: ClaimLocationViewModel by viewModel()
    private val frequencyModel: ClaimHeliumFrequencyViewModel by viewModel()
    private val resultModel: ClaimHeliumResultViewModel by viewModel()
    private val pairModel: ClaimHeliumPairViewModel by viewModel()
    private val photosViewModel: ClaimPhotosGalleryViewModel by viewModel()
    private lateinit var binding: ActivityClaimDeviceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClaimDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ClaimHeliumDevicePagerAdapter(this)
        binding.pager.adapter = pagerAdapter
        binding.pager.isUserInputEnabled = false
        binding.progress.max = PAGE_COUNT - 1

        onBackPressedDispatcher.addCallback {
            finishClaiming()
        }

        binding.toolbar.title = getString(R.string.title_claim_helium)
        binding.toolbar.setNavigationOnClickListener {
            finishClaiming()
        }

        model.onCancel().observe(this) {
            if (it) finishClaiming()
        }

        model.onNext().observe(this) {
            if (it) onNextPressed()
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
            model.setDeviceEUI(it.getString(DEV_EUI, String.empty()))
            model.setDeviceKey(it.getString(DEV_KEY, String.empty()))
            model.setClaimedDevice(it.parcelable(CLAIMED_DEVICE))
        }
        binding.progress.progress = binding.pager.currentItem + 1
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.CLAIM_HELIUM, classSimpleName())
    }

    private fun finishClaiming() {
        resultModel.disconnectFromPeripheral()
        finish()
    }

    private fun onNextPressed() {
        with(binding) {
            pager.currentItem += 1
            binding.progress.progress = binding.pager.currentItem + 1

            when (pager.currentItem) {
                PAGE_LOCATION -> locationModel.requestUserLocation()
                PAGE_PHOTOS_GALLERY -> photosViewModel.requestCameraPermission()
                PAGE_FREQUENCY -> {
                    frequencyModel.getCountryAndFrequencies(locationModel.getInstallationLocation())
                }
                PAGE_RESULT -> {
                    binding.appBar.visible(false)
                    binding.progress.visible(false)
                    resultModel.setSelectedDevice(pairModel.getSelectedDevice())
                    resultModel.setFrequency(model.getFrequency())
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_PAGE, binding.pager.currentItem)
        outState.putString(DEV_EUI, model.getDevEUI())
        outState.putString(DEV_KEY, model.getDeviceKey())
        model.onClaimResult().value?.data?.let {
            outState.putParcelable(CLAIMED_DEVICE, it)
        }
        super.onSaveInstanceState(outState)
    }

    private class ClaimHeliumDevicePagerAdapter(
        activity: AppCompatActivity,
    ) : FragmentStateAdapter(activity) {
        companion object {
            const val PAGE_BEFORE_CLAIMING = 0
            const val PAGE_RESET = 1
            const val PAGE_VERIFY_OR_PAIR = 2
            const val PAGE_LOCATION = 3
            const val PAGE_PHOTOS_INTRO = 4
            const val PAGE_PHOTOS_GALLERY = 5
            const val PAGE_FREQUENCY = 6
            const val PAGE_RESULT = 7
            const val PAGE_COUNT = 8
        }

        override fun getItemCount(): Int = PAGE_COUNT

        @Suppress("UseCheckOrError")
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                PAGE_BEFORE_CLAIMING -> ClaimBeforeYouClaimFragment.newInstance(DeviceType.HELIUM)
                PAGE_RESET -> ClaimHeliumResetFragment()
                PAGE_VERIFY_OR_PAIR -> ClaimHeliumPairFragment()
                PAGE_LOCATION -> ClaimLocationFragment.newInstance(DeviceType.HELIUM)
                PAGE_PHOTOS_INTRO -> ClaimPhotosIntroFragment.newInstance(DeviceType.HELIUM)
                PAGE_PHOTOS_GALLERY -> ClaimPhotosGalleryFragment.newInstance(DeviceType.HELIUM)
                PAGE_FREQUENCY -> ClaimHeliumFrequencyFragment()
                PAGE_RESULT -> ClaimHeliumResultFragment()
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
