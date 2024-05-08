package com.weatherxm.ui.claimdevice.selectstation

import android.os.Bundle
import com.weatherxm.databinding.ActivityClaimSelectStationBinding
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.Analytics

class SelectStationTypeActivity : BaseActivity() {
    private lateinit var binding: ActivityClaimSelectStationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClaimSelectStationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.m5WifiCard.listener {
            navigator.showClaimM5Flow(this)
        }

        binding.d1WifiCard.listener {
            // Coming Soon...
        }

        binding.heliumCard.listener {
            navigator.showClaimHeliumFlow(this)
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Analytics.Screen.CLAIM_DEVICE_TYPE_SELECTION, this::class.simpleName)
    }
}
