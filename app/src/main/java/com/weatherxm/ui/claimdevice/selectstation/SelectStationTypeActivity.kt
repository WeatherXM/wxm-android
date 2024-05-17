package com.weatherxm.ui.claimdevice.selectstation

import android.os.Bundle
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityClaimSelectStationBinding
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.components.BaseActivity

class SelectStationTypeActivity : BaseActivity() {
    private lateinit var binding: ActivityClaimSelectStationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClaimSelectStationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.m5WifiCard.listener {
            navigator.showClaimWifiFlow(this, DeviceType.M5_WIFI)
        }

        binding.d1WifiCard.listener {
            navigator.showClaimWifiFlow(this, DeviceType.D1_WIFI)
        }

        binding.heliumCard.listener {
            navigator.showClaimHeliumFlow(this)
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            AnalyticsService.Screen.CLAIM_DEVICE_TYPE_SELECTION, classSimpleName()
        )
    }
}
