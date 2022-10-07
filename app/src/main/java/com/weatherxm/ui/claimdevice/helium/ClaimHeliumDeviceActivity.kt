package com.weatherxm.ui.claimdevice.helium

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.weatherxm.databinding.ActivityClaimHeliumDeviceBinding
import com.weatherxm.ui.claimdevice.ClaimDeviceLocationFragment
import com.weatherxm.ui.claimdevice.helium.verify.ClaimHeliumDeviceVerifyFragment
import com.weatherxm.util.applyInsets

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
            if (it) binding.pager.currentItem += 1
        }
    }

    private class ClaimHeliumDevicePagerAdapter(
        activity: AppCompatActivity
    ) : FragmentStateAdapter(activity) {
        companion object {
            const val PAGE_COUNT = 3
        }

        override fun getItemCount(): Int = PAGE_COUNT

        @Suppress("UseCheckOrError")
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ClaimHeliumDeviceVerifyFragment()
                1 -> ClaimDeviceLocationFragment()
                2 -> ClaimDeviceLocationFragment()
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
