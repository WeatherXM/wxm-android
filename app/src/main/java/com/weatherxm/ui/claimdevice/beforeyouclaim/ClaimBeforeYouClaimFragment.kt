package com.weatherxm.ui.claimdevice.beforeyouclaim

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.res.dimensionResource
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimBeforeYouClaimBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseViewModel
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiViewModel
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_TYPE
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.compose.TextWithStartingIcon
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimBeforeYouClaimFragment : BaseFragment() {
    private val heliumParentModel: ClaimHeliumViewModel by activityViewModel()
    private val wifiParentModel: ClaimWifiViewModel by activityViewModel()
    private val pulseParentModel: ClaimPulseViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimBeforeYouClaimBinding

    companion object {
        fun newInstance(deviceType: DeviceType) = ClaimBeforeYouClaimFragment().apply {
            arguments = Bundle().apply { putParcelable(ARG_DEVICE_TYPE, deviceType) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimBeforeYouClaimBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deviceType = arguments?.parcelable<DeviceType>(ARG_DEVICE_TYPE)
        if (context == null || deviceType == null) {
            // No point executing if in the meanwhile the activity is dead
            return
        }

        binding.previousStepsView.setContent {
            Column(
                verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_small_to_normal))
            ) {
                TextWithStartingIcon(text = getString(R.string.check_box_contents))
                TextWithStartingIcon(
                    text = getString(R.string.assemble_weather_station),
                    iconRes = R.drawable.ic_two_filled
                )
                if (deviceType != DeviceType.HELIUM) {
                    TextWithStartingIcon(
                        text = getString(R.string.install_weather_station_following_guidelines),
                        iconRes = R.drawable.ic_three_filled
                    )
                }
            }
        }

        binding.nextStepsView.setContent {
            Column(
                verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_small_to_normal))
            ) {
                if (deviceType == DeviceType.HELIUM) {
                    TextWithStartingIcon(
                        text = getString(R.string.pair_station_via_bluetooth),
                        iconRes = R.drawable.ic_three_filled
                    )
                    TextWithStartingIcon(
                        text = getString(R.string.install_weather_station_following_guidelines),
                        iconRes = R.drawable.ic_four_filled
                    )
                } else {
                    TextWithStartingIcon(
                        text = getString(R.string.connect_gateway_ready_for_claiming),
                        iconRes = R.drawable.ic_four_filled
                    )
                }
                TextWithStartingIcon(
                    text = getString(R.string.confirm_station_exact_deployment_location),
                    iconRes = R.drawable.ic_five_filled
                )
                TextWithStartingIcon(
                    text = getString(R.string.take_photos_station_deployment_guidelines),
                    iconRes = R.drawable.ic_six_filled
                )
                if (deviceType == DeviceType.HELIUM) {
                    TextWithStartingIcon(
                        text = getString(R.string.set_your_station_frequency),
                        iconRes = R.drawable.ic_seven_filled
                    )
                    TextWithStartingIcon(
                        text = getString(R.string.all_done_enjoy_station_earn_rewards),
                        iconRes = R.drawable.ic_eight_filled
                    )
                } else {
                    TextWithStartingIcon(
                        text = getString(R.string.all_done_enjoy_station_earn_rewards),
                        iconRes = R.drawable.ic_seven_filled
                    )
                }
            }
        }

        binding.beginStationClaimingBtn.setOnClickListener {
            when (deviceType) {
                DeviceType.M5_WIFI, DeviceType.D1_WIFI -> wifiParentModel.next()
                DeviceType.PULSE_4G -> pulseParentModel.next()
                DeviceType.HELIUM -> heliumParentModel.next()
            }
        }
    }
}
