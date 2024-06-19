package com.weatherxm.ui.claimdevice.wifi.result

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentClaimWifiResultBinding
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiViewModel
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimWifiResultFragment : BaseFragment() {
    private val parentModel: ClaimWifiViewModel by activityViewModel()
    private val locationModel: ClaimLocationViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimWifiResultBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimWifiResultBinding.inflate(inflater, container, false)
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
            parentModel.cancel()
        }

        binding.retry.setOnClickListener {
            parentModel.claimDevice(locationModel.getInstallationLocation())
        }

        parentModel.onClaimResult().observe(viewLifecycleOwner) {
            updateUI(it)
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
                    .title(R.string.error_claim_failed_title)
                    .action(getString(R.string.contact_support_title))
                    .listener {
                        navigator.openSupportCenter(context)
                    }
                resource.message?.let {
                    binding.statusView.htmlSubtitle(it) {
                        navigator.openSupportCenter(context)
                    }
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
