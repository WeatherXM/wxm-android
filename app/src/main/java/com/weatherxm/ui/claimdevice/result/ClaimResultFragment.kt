package com.weatherxm.ui.claimdevice.result

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.FragmentClaimResultBinding
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseViewModel
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiViewModel
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_TYPE
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.DeviceType.PULSE_4G
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimResultFragment : BaseFragment() {
    private val wifiParentModel: ClaimWifiViewModel by activityViewModel()
    private val pulseParentModel: ClaimPulseViewModel by activityViewModel()
    private val locationModel: ClaimLocationViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimResultBinding
    private lateinit var deviceType: DeviceType

    companion object {
        const val TAG = "ClaimResultFragment"

        fun newInstance(deviceType: DeviceType) = ClaimResultFragment().apply {
            arguments =
                Bundle().apply { putParcelable(ARG_DEVICE_TYPE, deviceType) }
        }
    }

    init {
        lifecycleScope.launch {
            whenCreated {
                arguments?.parcelable<DeviceType>(ARG_DEVICE_TYPE)?.let {
                    deviceType = it
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cancel.setOnClickListener {
            analytics.trackEventUserAction(
                actionName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
                contentType = AnalyticsService.ParamValue.CLAIMING.paramValue,
                Pair(
                    AnalyticsService.CustomParam.ACTION.paramName,
                    AnalyticsService.ParamValue.QUIT.paramValue
                )
            )
            if (deviceType == PULSE_4G) {
                pulseParentModel.cancel()
            } else {
                wifiParentModel.cancel()
            }
        }

        binding.retry.setOnClickListener {
            if (deviceType == PULSE_4G) {
                pulseParentModel.claimDevice(locationModel.getInstallationLocation())
            } else {
                wifiParentModel.claimDevice(locationModel.getInstallationLocation())
            }
        }

        if (deviceType == PULSE_4G) {
            pulseParentModel.onClaimResult().observe(viewLifecycleOwner) {
                updateUI(it)
            }
        } else {
            wifiParentModel.onClaimResult().observe(viewLifecycleOwner) {
                updateUI(it)
            }
        }
    }

    private fun updateUI(resource: Resource<UIDevice>) {
        binding.cancel.visible(resource.status == Status.ERROR)
        binding.retry.visible(resource.status == Status.ERROR)
        when (resource.status) {
            Status.SUCCESS -> {
                binding.statusView.animation(R.raw.anim_success, false)
                    .title(R.string.station_claimed)
                    .htmlSubtitle(
                        R.string.success_claim_device,
                        resource.data?.name
                    )
                val device = resource.data
                if (device != null) {
                    binding.goToStationBtn.setOnClickListener {
                        analytics.trackEventUserAction(
                            actionName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
                            contentType = AnalyticsService.ParamValue.CLAIMING.paramValue,
                            Pair(
                                AnalyticsService.CustomParam.ACTION.paramName,
                                AnalyticsService.ParamValue.VIEW_STATION.paramValue
                            )
                        )
                        navigator.showDeviceDetails(activity, device = device)
                        activity?.setResult(Activity.RESULT_OK)
                        activity?.finish()
                    }
                    binding.goToStationBtn.visible(true)
                }
                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
                    contentId = AnalyticsService.ParamValue.CLAIMING_RESULT_ID.paramValue,
                    success = 1L
                )
            }
            Status.ERROR -> {
                binding.statusView.animation(R.raw.anim_error, false)
                    .title(R.string.error_generic_message)
                    .action(getString(R.string.contact_support_title))
                    .subtitle(resource.message)
                    .listener {
                        navigator.openSupportCenter(context)
                    }
                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
                    contentId = AnalyticsService.ParamValue.CLAIMING_RESULT_ID.paramValue,
                    success = 0L
                )
            }
            Status.LOADING -> {
                binding.statusView.clear()
                    .animation(R.raw.anim_loading)
                    .title(R.string.claiming_station)
                    .htmlSubtitle(R.string.claiming_station_m5_desc)
            }
        }
    }
}
