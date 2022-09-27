package com.weatherxm.ui.claimdevice.helium

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.weatherxm.databinding.ActivityClaimHeliumDeviceBinding
import com.weatherxm.ui.claimdevice.helium.verify.ClaimHeliumDeviceVerifyFragment
import com.weatherxm.util.applyInsets

class ClaimHeliumDeviceActivity : AppCompatActivity() {
    private val model: ClaimHeliumDeviceViewModel by viewModels()
    private lateinit var binding: ActivityClaimHeliumDeviceBinding

    companion object {
        const val ARG_IS_MANUAL_CLAIMING = "is_manual_claiming"
        const val ARG_DEVICE_BLE_ADDRESS = "device_ble_address"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClaimHeliumDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        model.setManualClaiming(intent?.extras?.getBoolean(ARG_IS_MANUAL_CLAIMING))
        model.setDeviceAddress(intent?.extras?.getString(ARG_DEVICE_BLE_ADDRESS))

        binding.root.applyInsets()

        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ClaimHeliumDevicePagerAdapter(this)
        binding.pager.adapter = pagerAdapter
        binding.pager.isUserInputEnabled = false

        onBackPressedDispatcher.addCallback {
            finish()
        }

        model.onCancel().observe(this) {
            if (it) finish()
        }
    }

    private class ClaimHeliumDevicePagerAdapter(
        activity: AppCompatActivity
    ) : FragmentStateAdapter(activity) {
        companion object {
            const val PAGE_VERIFY = 0
            const val PAGE_COUNT = 1
        }

        override fun getItemCount(): Int = PAGE_COUNT

        @Suppress("UseCheckOrError")
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                PAGE_VERIFY -> ClaimHeliumDeviceVerifyFragment()
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
