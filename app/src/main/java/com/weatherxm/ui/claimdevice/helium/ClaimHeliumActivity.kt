package com.weatherxm.ui.claimdevice.helium

import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityClaimHeliumDeviceBinding
import com.weatherxm.ui.claimdevice.helium.frequency.ClaimHeliumFrequencyFragment
import com.weatherxm.ui.claimdevice.helium.frequency.ClaimHeliumFrequencyViewModel
import com.weatherxm.ui.claimdevice.helium.pair.ClaimHeliumPairFragment
import com.weatherxm.ui.claimdevice.helium.pair.ClaimHeliumPairViewModel
import com.weatherxm.ui.claimdevice.helium.reset.ClaimHeliumResetFragment
import com.weatherxm.ui.claimdevice.helium.result.ClaimHeliumResultFragment
import com.weatherxm.ui.claimdevice.helium.result.ClaimHeliumResultViewModel
import com.weatherxm.ui.claimdevice.location.ClaimLocationFragment
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setIcon
import com.weatherxm.ui.common.setSuccessChip
import com.weatherxm.ui.components.BaseActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class ClaimHeliumActivity : BaseActivity() {
    companion object {
        const val CURRENT_PAGE = "current_page"
        const val DEV_EUI = "dev_eui"
        const val DEV_KEY = "dev_key"
        const val CLAIMED_DEVICE = "claimed_device"
    }

    private val model: ClaimHeliumViewModel by viewModel()
    private val locationModel: ClaimLocationViewModel by viewModel()
    private val frequencyModel: ClaimHeliumFrequencyViewModel by viewModel()
    private val resultModel: ClaimHeliumResultViewModel by viewModel()
    private val pairModel: ClaimHeliumPairViewModel by viewModel()
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
            finishClaiming()
        }

        binding.toolbar.setNavigationOnClickListener {
            finishClaiming()
        }

        model.onCancel().observe(this) {
            if (it) finishClaiming()
        }

        model.onNext().observe(this) {
            if (it) onNextPressed()
        }

        model.onBackToLocation().observe(this) {
            if (it) {
                binding.pager.currentItem -= 1
                binding.thirdStep.setIcon(R.drawable.ic_three_filled)
                binding.fourthStep.setIcon(R.drawable.ic_four_outlined)
            }
        }

        savedInstanceState?.let {
            binding.pager.currentItem = it.getInt(CURRENT_PAGE, 0)
            model.setDeviceEUI(it.getString(DEV_EUI, String.empty()))
            model.setDeviceKey(it.getString(DEV_KEY, String.empty()))
            model.setClaimedDevice(it.parcelable(CLAIMED_DEVICE))
        }
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

            when (pager.currentItem) {
                ClaimHeliumDevicePagerAdapter.PAGE_VERIFY_OR_PAIR -> {
                    firstStep.setSuccessChip()
                    secondStep.setIcon(R.drawable.ic_two_filled)
                }
                ClaimHeliumDevicePagerAdapter.PAGE_LOCATION -> {
                    secondStep.setSuccessChip()
                    thirdStep.setIcon(R.drawable.ic_three_filled)
                    locationModel.requestUserLocation()
                }
                ClaimHeliumDevicePagerAdapter.PAGE_FREQUENCY -> {
                    thirdStep.setSuccessChip()
                    fourthStep.setIcon(R.drawable.ic_four_filled)
                    frequencyModel.getCountryAndFrequencies(locationModel.getInstallationLocation())
                }
                ClaimHeliumDevicePagerAdapter.PAGE_RESULT -> {
                    resultModel.setSelectedDevice(pairModel.getSelectedDevice())
                    resultModel.setFrequency(model.getFrequency())
                    fourthStep.setSuccessChip()
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
                PAGE_RESULT -> ClaimHeliumResultFragment()
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
