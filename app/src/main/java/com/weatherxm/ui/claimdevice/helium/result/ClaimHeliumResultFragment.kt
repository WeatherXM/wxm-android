package com.weatherxm.ui.claimdevice.helium.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.DeviceProfile.Helium
import com.weatherxm.data.DeviceProfile.M5
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentClaimHeliumResultBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseFragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimHeliumResultFragment : BaseFragment() {
    private val parentModel: ClaimHeliumViewModel by activityViewModel()
    private val locationModel: ClaimLocationViewModel by activityViewModel()
    private val model: ClaimHeliumResultViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimHeliumResultBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimHeliumResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()

        model.onRebooting().observe(viewLifecycleOwner) {
            if (it) {
                binding.bleActionFlow.onStep(
                    1, R.string.claiming_station, R.string.claiming_station_helium_desc
                )
            }
        }

        model.onBLEConnection().observe(viewLifecycleOwner) {
            if (it) {
                binding.bleActionFlow.onStep(
                    2, R.string.claiming_station, R.string.claiming_station_helium_desc
                )
            }
        }

        model.onBLEDevEUI().observe(viewLifecycleOwner) {
            parentModel.setDeviceEUI(it)
        }

        model.onBLEClaimingKey().observe(viewLifecycleOwner) {
            parentModel.setDeviceKey(it)
            parentModel.claimDevice(locationModel.getInstallationLocation())
        }

        model.onBLEError().observe(viewLifecycleOwner) { uiError ->
            binding.bleActionFlow.setRetryButtonListener {
                uiError.retryFunction?.invoke()
            }
            binding.bleActionFlow.onError(
                false,
                title = R.string.error_claim_failed_title,
                retryActionText = getString(R.string.action_retry),
                message = uiError.errorMessage,
                errorCode = uiError.errorCode
            ) {
                navigator.openSupportCenter(context = context)
            }
        }

        parentModel.onClaimResult().observe(viewLifecycleOwner) {
            updateUI(it)
        }

        binding.bleActionFlow.onStep(
            0, R.string.claiming_station, R.string.claiming_station_helium_desc
        )
    }

    private fun setListeners() {
        binding.bleActionFlow.setListeners(onScanClicked = {
            // Not used
        }, onPairClicked = {
            // Not used
        }, onSuccessPrimaryButtonClicked = {
            // We will define this later when we show it
        }, onCancelButtonClicked = {
            analytics.trackEventUserAction(
                actionName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
                contentType = AnalyticsService.ParamValue.CLAIMING.paramValue,
                Pair(
                    AnalyticsService.CustomParam.ACTION.paramName,
                    AnalyticsService.ParamValue.CANCEL.paramValue
                )
            )
            parentModel.cancel()
        }, onRetryButtonClicked = {
            // We will define this later when we show it
        })
    }

    private fun updateUI(resource: Resource<UIDevice>) {
        when (resource.status) {
            Status.SUCCESS -> {
                val device = resource.data
                if (device != null && device.profile == Helium && device.needsUpdate()) {
                    analytics.trackEventPrompt(
                        AnalyticsService.ParamValue.OTA_AVAILABLE.paramValue,
                        AnalyticsService.ParamValue.WARN.paramValue,
                        AnalyticsService.ParamValue.VIEW.paramValue
                    )
                    binding.bleActionFlow.setSuccessPrimaryButtonListener {
                        onUpdate(device)
                    }
                    binding.bleActionFlow.setSuccessSecondaryButtonListener {
                        showConfirmBypassOTADialog(device)
                    }
                    binding.bleActionFlow.onSuccess(
                        R.string.station_claimed,
                        message = null,
                        htmlMessage = getString(R.string.success_claim_device),
                        argForHtmlMessage = device.name,
                        primaryActionText = getString(R.string.action_update_firmware),
                        secondaryActionText = getString(R.string.action_view_station)
                    )
                    binding.bleActionFlow.onShowInformationCard()
                } else if (device != null && (device.profile == M5 || !device.needsUpdate())) {
                    binding.bleActionFlow.setSuccessOneButtonOnlyListener {
                        onViewDevice(device)
                    }
                    binding.bleActionFlow.onSuccess(
                        R.string.station_claimed,
                        message = null,
                        htmlMessage = getString(R.string.success_claim_device),
                        argForHtmlMessage = device.name,
                        primaryActionText = getString(R.string.action_view_station)
                    )
                }
                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
                    contentId = AnalyticsService.ParamValue.CLAIMING_RESULT_ID.paramValue,
                    success = 1L
                )
            }
            Status.ERROR -> {
                binding.bleActionFlow.setRetryButtonListener {
                    analytics.trackEventUserAction(
                        actionName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
                        contentType = AnalyticsService.ParamValue.CLAIMING.paramValue,
                        Pair(
                            AnalyticsService.CustomParam.ACTION.paramName,
                            AnalyticsService.ParamValue.RETRY.paramValue
                        )
                    )
                    binding.bleActionFlow.onStep(
                        2, R.string.claiming_station, R.string.claiming_station_helium_desc
                    )
                    parentModel.claimDevice(locationModel.getInstallationLocation())
                }
                binding.bleActionFlow.onError(
                    false,
                    R.string.error_claim_failed_title,
                    getString(R.string.action_retry_claiming),
                    resource.message,
                    resource.error?.code
                ) {
                    navigator.openSupportCenter(context = context)
                }
                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
                    contentId = AnalyticsService.ParamValue.CLAIMING_RESULT_ID.paramValue,
                    success = 0L
                )
            }
            Status.LOADING -> {
                // Do nothing
            }
        }
    }

    private fun onViewDevice(device: UIDevice) {
        analytics.trackEventUserAction(
            actionName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
            contentType = AnalyticsService.ParamValue.CLAIMING.paramValue,
            Pair(
                AnalyticsService.CustomParam.ACTION.paramName,
                AnalyticsService.ParamValue.VIEW_STATION.paramValue
            )
        )
        model.disconnectFromPeripheral()
        navigator.showDeviceDetails(activity, device = device)
        activity?.finish()
    }

    private fun onUpdate(device: UIDevice) {
        analytics.trackEventPrompt(
            AnalyticsService.ParamValue.OTA_AVAILABLE.paramValue,
            AnalyticsService.ParamValue.WARN.paramValue,
            AnalyticsService.ParamValue.ACTION.paramValue
        )
        navigator.showDeviceHeliumOTA(this, device, true)
        activity?.finish()
    }

    private fun showConfirmBypassOTADialog(device: UIDevice) {
        ActionDialogFragment
            .Builder(
                title = getString(R.string.action_update_firmware),
                message = getString(R.string.update_prompt_on_dialog)
            )
            .onNegativeClick(getString(R.string.action_view_station)) {
                onViewDevice(device)
            }
            .onPositiveClick(getString(R.string.update)) {
                onUpdate(device)
            }
            .build()
            .show(this)
    }
}
