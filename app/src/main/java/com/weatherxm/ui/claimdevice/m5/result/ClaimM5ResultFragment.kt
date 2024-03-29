package com.weatherxm.ui.claimdevice.m5.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentClaimM5ResultBinding
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.m5.ClaimM5ViewModel
import com.weatherxm.ui.claimdevice.m5.verify.ClaimM5VerifyViewModel
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.util.Analytics
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimM5ResultFragment : BaseFragment() {
    private val m5ParentModel: ClaimM5ViewModel by activityViewModel()
    private val verifyM5Model: ClaimM5VerifyViewModel by activityViewModel()
    private val locationModel: ClaimLocationViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimM5ResultBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimM5ResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.quit.setOnClickListener {
            analytics.trackEventUserAction(
                actionName = Analytics.ParamValue.CLAIMING_RESULT.paramValue,
                contentType = Analytics.ParamValue.CLAIMING.paramValue,
                Pair(
                    Analytics.CustomParam.ACTION.paramName, Analytics.ParamValue.QUIT.paramValue
                )
            )
            m5ParentModel.cancel()
        }

        binding.retry.setOnClickListener {
            m5ParentModel.claimDevice(
                verifyM5Model.getSerialNumber(),
                locationModel.getInstallationLocation()
            )
        }

        m5ParentModel.onClaimResult().observe(viewLifecycleOwner) {
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
                            actionName = Analytics.ParamValue.CLAIMING_RESULT.paramValue,
                            contentType = Analytics.ParamValue.CLAIMING.paramValue,
                            Pair(
                                Analytics.CustomParam.ACTION.paramName,
                                Analytics.ParamValue.VIEW_STATION.paramValue
                            )
                        )
                        navigator.showDeviceDetails(activity, device = device)
                        activity?.finish()
                    }
                    binding.viewStationOnlyBtn.visibility = View.VISIBLE
                }
                binding.failureButtons.visibility = View.GONE
                analytics.trackEventViewContent(
                    contentName = Analytics.ParamValue.CLAIMING_RESULT.paramValue,
                    contentId = Analytics.ParamValue.CLAIMING_RESULT_ID.paramValue,
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
                    contentName = Analytics.ParamValue.CLAIMING_RESULT.paramValue,
                    contentId = Analytics.ParamValue.CLAIMING_RESULT_ID.paramValue,
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
