package com.weatherxm.ui.claimdevice.wifi.connectwifi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimWifiConnectWifiBinding
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiViewModel
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.components.BaseFragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimWifiConnectWifiFragment : BaseFragment() {
    private val parentModel: ClaimWifiViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimWifiConnectWifiBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimWifiConnectWifiBinding.inflate(inflater, container, false)

        if (parentModel.deviceType == DeviceType.M5_WIFI) {
            binding.firstStep.setHtml(R.string.connect_m5_wifi_first_step)
            binding.secondStep.setHtml(R.string.connect_m5_wifi_second_step)

            binding.watchVideoButton.setOnClickListener {
                navigator.openWebsite(
                    this.context,
                    getString(R.string.m5_connect_wifi_instructional_video)
                )
            }
        } else {
            binding.firstStep.setHtml(R.string.connect_d1_wifi_first_step)
            binding.secondStep.setHtml(R.string.connect_d1_wifi_second_step)
        }
        binding.watchVideoText.setVisible(parentModel.deviceType == DeviceType.M5_WIFI)
        binding.watchVideoButton.setVisible(parentModel.deviceType == DeviceType.M5_WIFI)
        binding.thirdStep.setHtml(R.string.connect_wifi_third_step)

        binding.connectButton.setOnClickListener {
            parentModel.next()
        }

        return binding.root
    }
}
