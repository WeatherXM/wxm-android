package com.weatherxm.ui.claimdevice.selectstation

import android.app.Activity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityClaimSelectStationBinding
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.components.BaseActivity

class SelectStationTypeActivity : BaseActivity() {
    private lateinit var binding: ActivityClaimSelectStationBinding

    // Register the launcher for the edit location activity and wait for a possible result
    private val claimingLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClaimSelectStationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.m5WifiCard.listener {
            navigator.showClaimWifiFlow(claimingLauncher, this, DeviceType.M5_WIFI)
        }

        binding.d1WifiCard.listener {
            navigator.showClaimWifiFlow(claimingLauncher, this, DeviceType.D1_WIFI)
        }

        binding.heliumCard.listener {
            navigator.showClaimHeliumFlow(claimingLauncher, this)
        }

        binding.pulseCard.listener {
            navigator.showClaimPulseFlow(claimingLauncher, this)
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            AnalyticsService.Screen.CLAIM_DEVICE_TYPE_SELECTION, classSimpleName()
        )
    }
}
