package com.weatherxm.ui.claimdevice.wifi.preparegateway

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import coil.request.ImageRequest
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimWifiPrepareClaimingBinding
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiViewModel
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.components.BaseFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimWifiPrepareGatewayFragment : BaseFragment() {
    private val parentModel: ClaimWifiViewModel by activityViewModel()
    private val imageLoader: ImageLoader by inject()
    private lateinit var binding: FragmentClaimWifiPrepareClaimingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimWifiPrepareClaimingBinding.inflate(inflater, container, false)

        if (parentModel.deviceType == DeviceType.M5_WIFI) {
            binding.firstStep.setHtml(R.string.prepare_gateway_m1_first_step)
            binding.m5Notice.setHtml(R.string.prepare_gateway_m5_notice)

//            imageLoader.enqueue(
//                ImageRequest.Builder(requireContext())
//                    .data()
//                    .target(binding.guideGif)
//                    .build()
//            )
        } else {
            binding.firstStep.setHtml(R.string.prepare_gateway_d1_first_step)

//            imageLoader.enqueue(
//                ImageRequest.Builder(requireContext())
//                    .data()
//                    .target(binding.guideGif)
//                    .build()
//            )
        }
        binding.secondStep.setHtml(R.string.prepare_gateway_wifi_second_step)
        binding.m5Notice.setVisible(parentModel.deviceType == DeviceType.M5_WIFI)

        binding.enterManuallyBtn.setOnClickListener {
            // To be implemented
        }

        binding.scanBtn.setOnClickListener {
            // To be implemented
        }

        return binding.root
    }
}
