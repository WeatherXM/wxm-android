package com.weatherxm.ui.claimdevice.selectstation

import android.os.Bundle
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityClaimSelectStationBinding
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.components.BaseActivity

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
            finish()
        }

        binding.d1WifiCard.listener {
            // Coming Soon...
        }

        binding.heliumCard.listener {
            navigator.showClaimHeliumFlow(this)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            AnalyticsService.Screen.CLAIM_DEVICE_TYPE_SELECTION, classSimpleName()
        )
    }
}
