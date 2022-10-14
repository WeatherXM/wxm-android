package com.weatherxm.ui.claimdevice.helium

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.weatherxm.R
import com.weatherxm.databinding.ActivityClaimHeliumDeviceBinding
import com.weatherxm.ui.claimdevice.ClaimDeviceLocationFragment
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumDeviceActivity.ClaimHeliumDevicePagerAdapter.Companion.PAGE_LOCATION
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumDeviceActivity.ClaimHeliumDevicePagerAdapter.Companion.PAGE_RESET
import com.weatherxm.ui.claimdevice.helium.reset.ClaimHeliumDeviceResetFragment
import com.weatherxm.ui.claimdevice.helium.verify.ClaimHeliumDeviceVerifyFragment
import com.weatherxm.util.applyInsets
import com.weatherxm.util.setIcon
import com.weatherxm.util.setIconAndColor

class ClaimHeliumDeviceActivity : AppCompatActivity() {
    private val model: ClaimHeliumDeviceViewModel by viewModels()
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
            with(binding) {
                if (it) pager.currentItem += 1

                when (pager.currentItem) {
                    PAGE_RESET -> {
                        verify.setIconAndColor(R.drawable.ic_checkmark, R.color.success_tint)
                        verify.setChipIconTintResource(R.color.dark_background)
                        verify.setTextColor(getColor(R.color.dark_background))
                        resetAndPair.setIcon(R.drawable.ic_two_filled)
                    }
                    PAGE_LOCATION -> {
                        resetAndPair.setIconAndColor(R.drawable.ic_checkmark, R.color.success_tint)
                        resetAndPair.setChipIconTintResource(R.color.dark_background)
                        resetAndPair.setTextColor(getColor(R.color.dark_background))
                        location.setIcon(R.drawable.ic_three_filled)
                    }
                }
            }
        }
    }

    private class ClaimHeliumDevicePagerAdapter(
        activity: AppCompatActivity
    ) : FragmentStateAdapter(activity) {
        companion object {
            const val PAGE_VERIFY = 0
            const val PAGE_RESET = 1
            const val PAGE_LOCATION = 2
            const val PAGE_COUNT = 3
        }

        override fun getItemCount(): Int = PAGE_COUNT

        @Suppress("UseCheckOrError")
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ClaimHeliumDeviceVerifyFragment()
                1 -> ClaimHeliumDeviceResetFragment()
                2 -> ClaimDeviceLocationFragment()
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
