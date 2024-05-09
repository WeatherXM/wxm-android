package com.weatherxm.ui.claimdevice.wifi.result

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
import com.weatherxm.ui.claimdevice.wifi.verify.ClaimWifiVerifyViewModel
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.components.BaseFragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimWifiResultFragment : BaseFragment() {
    private val parentModel: ClaimWifiViewModel by activityViewModel()
    private val verifyModel: ClaimWifiVerifyViewModel by activityViewModel()
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

        binding.quit.setOnClickListener {
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
            parentModel.claimDevice(
                verifyModel.getSerialNumber(),
                locationModel.getInstallationLocation()
            )
        }

        parentModel.onClaimResult().observe(viewLifecycleOwner) {
            updateUI(it)
        }
    }

    private fun updateUI(resource: Resource<UIDevice>) {
        when (resource.status) {
            Status.SUCCESS -> {
                binding.statusView.animation(R.raw.anim_success, false)
                binding.statusView.title(R.string.station_claimed)
                binding.statusView.htmlSubtitle(
                    R.string.success_claim_device,
                    resource.data?.name
                )
                val device = resource.data
                if (device != null) {
                    binding.viewStationOnlyBtn.setOnClickListener {
                        analytics.trackEventUserAction(
                            actionName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
                            contentType = AnalyticsService.ParamValue.CLAIMING.paramValue,
                            Pair(
                                AnalyticsService.CustomParam.ACTION.paramName,
                                AnalyticsService.ParamValue.VIEW_STATION.paramValue
                            )
                        )
                        navigator.showDeviceDetails(activity, device = device)
                        activity?.finish()
                    }
                    binding.viewStationOnlyBtn.visibility = View.VISIBLE
                }
                binding.failureButtons.visibility = View.GONE
                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
                    contentId = AnalyticsService.ParamValue.CLAIMING_RESULT_ID.paramValue,
                    success = 1L
                )
            }
            Status.ERROR -> {
                binding.statusView.animation(R.raw.anim_error, false)
                binding.statusView.title(R.string.error_claim_failed_title)
                resource.message?.let {
                    binding.statusView.htmlSubtitle(it) {
                        navigator.openSupportCenter(context)
                    }
                }
                binding.statusView.action(getString(R.string.contact_support_title))
                binding.statusView.listener {
                    navigator.openSupportCenter(context)
                }
                binding.failureButtons.visibility = View.VISIBLE
                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
                    contentId = AnalyticsService.ParamValue.CLAIMING_RESULT_ID.paramValue,
                    success = 0L
                )
            }
            Status.LOADING -> {
                binding.statusView.clear()
                binding.statusView.animation(R.raw.anim_loading)
                binding.statusView.title(R.string.claiming_station)
                binding.failureButtons.visibility = View.GONE
            }
        }
    }
}
