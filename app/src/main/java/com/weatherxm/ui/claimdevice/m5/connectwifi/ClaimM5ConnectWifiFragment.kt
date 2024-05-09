package com.weatherxm.ui.claimdevice.m5.connectwifi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimConnectWifiBinding
import com.weatherxm.ui.claimdevice.m5.ClaimM5ViewModel
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.components.BaseFragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimM5ConnectWifiFragment : BaseFragment() {
    private val parentModel: ClaimM5ViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimConnectWifiBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimConnectWifiBinding.inflate(inflater, container, false)

        binding.firstStep.setHtml(R.string.connect_m5_wifi_first_step)
        binding.secondStep.setHtml(R.string.connect_m5_wifi_second_step)
        binding.thirdStep.setHtml(R.string.connect_wifi_third_step)

        binding.watchVideoText.setVisible(true)
        binding.watchVideoButton.setVisible(true)
        binding.watchVideoButton.setOnClickListener {
            navigator.openWebsite(
                this.context,
                getString(R.string.m5_connect_wifi_instructional_video)
            )
        }

        binding.connectButton.setOnClickListener {
            parentModel.next()
        }

        return binding.root
    }
}
