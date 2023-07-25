package com.weatherxm.ui.claimdevice.helium.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceProfile.Helium
import com.weatherxm.data.DeviceProfile.M5
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentClaimHeliumResultBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.common.ActionDialogFragment
import com.weatherxm.util.Analytics
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClaimHeliumResultFragment : Fragment(), KoinComponent {
    private val parentModel: ClaimHeliumViewModel by activityViewModels()
    private val locationModel: ClaimLocationViewModel by activityViewModels()
    private val model: ClaimHeliumResultViewModel by activityViewModels()
    private lateinit var binding: FragmentClaimHeliumResultBinding
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()

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
                uiError.retryFunction
            }
            binding.bleActionFlow.onError(
                false,
                title = R.string.error_claim_failed_title,
                retryActionText = getString(R.string.action_retry),
                message = uiError.errorMessage,
                errorCode = uiError.errorCode
            ) {
                sendSupportEmail(it)
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
                actionName = Analytics.ParamValue.CLAIMING_RESULT.paramValue,
                contentType = Analytics.ParamValue.CLAIMING.paramValue,
                Pair(
                    Analytics.CustomParam.ACTION.paramName,
                    Analytics.ParamValue.CANCEL.paramValue
                )
            )
            parentModel.cancel()
        }, onRetryButtonClicked = {
            // We will define this later when we show it
        })
    }

    private fun updateUI(resource: Resource<Device>) {
        when (resource.status) {
            Status.SUCCESS -> {
                val device = resource.data
                if (device != null && device.profile == Helium && device.needsUpdate()) {
                    analytics.trackEventPrompt(
                        Analytics.ParamValue.OTA_AVAILABLE.paramValue,
                        Analytics.ParamValue.WARN.paramValue,
                        Analytics.ParamValue.VIEW.paramValue
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
                    contentName = Analytics.ParamValue.CLAIMING_RESULT.paramValue,
                    contentId = Analytics.ParamValue.CLAIMING_RESULT_ID.paramValue,
                    success = 1L
                )
            }
            Status.ERROR -> {
                binding.bleActionFlow.setRetryButtonListener {
                    analytics.trackEventUserAction(
                        actionName = Analytics.ParamValue.CLAIMING_RESULT.paramValue,
                        contentType = Analytics.ParamValue.CLAIMING.paramValue,
                        Pair(
                            Analytics.CustomParam.ACTION.paramName,
                            Analytics.ParamValue.RETRY.paramValue
                        )
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
                    sendSupportEmail(it)
                }
                analytics.trackEventViewContent(
                    contentName = Analytics.ParamValue.CLAIMING_RESULT.paramValue,
                    contentId = Analytics.ParamValue.CLAIMING_RESULT_ID.paramValue,
                    success = 0L
                )
            }
            Status.LOADING -> {
                // Do nothing
            }
        }
    }

    private fun sendSupportEmail(errorCode: String?) {
        navigator.sendSupportEmail(
            context = context,
            subject = getString(R.string.support_email_subject_cannot_claim),
            body = getString(
                R.string.support_email_body_user_and_helium_device_info,
                parentModel.getUserEmail(),
                parentModel.getDevEUI(),
                parentModel.getDeviceKey(),
                errorCode ?: getString(R.string.unknown)
            )
        )
    }

    private fun onViewDevice(device: Device) {
        analytics.trackEventUserAction(
            actionName = Analytics.ParamValue.CLAIMING_RESULT.paramValue,
            contentType = Analytics.ParamValue.CLAIMING.paramValue,
            Pair(
                Analytics.CustomParam.ACTION.paramName,
                Analytics.ParamValue.VIEW_STATION.paramValue
            )
        )
        parentModel.disconnectFromPeripheral()
        navigator.showDeviceDetails(activity, device = device)
        activity?.finish()
    }

    private fun onUpdate(device: Device) {
        analytics.trackEventPrompt(
            Analytics.ParamValue.OTA_AVAILABLE.paramValue,
            Analytics.ParamValue.WARN.paramValue,
            Analytics.ParamValue.ACTION.paramValue
        )
        navigator.showDeviceHeliumOTA(this, device, true)
        activity?.finish()
    }

    private fun showConfirmBypassOTADialog(device: Device) {
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
