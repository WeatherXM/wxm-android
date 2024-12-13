package com.weatherxm.ui.claimdevice.helium.result

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.FragmentClaimHeliumResultBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.show
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseFragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimHeliumResultFragment : BaseFragment() {
    private val parentModel: ClaimHeliumViewModel by activityViewModel()
    private val locationModel: ClaimLocationViewModel by activityViewModel()
    private val model: ClaimHeliumResultViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimHeliumResultBinding

    companion object {
        const val STEP_SET_UP_STATION = 0
        const val STEP_REBOOT_STATION = 1
        const val STEP_CLAIM_STATION = 2
    }

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

        binding.cancel.setOnClickListener {
            analytics.trackEventUserAction(
                actionName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
                contentType = AnalyticsService.ParamValue.CLAIMING.paramValue,
                Pair(
                    AnalyticsService.CustomParam.ACTION.paramName,
                    AnalyticsService.ParamValue.CANCEL.paramValue
                )
            )
            parentModel.cancel()
        }

        model.onRebooting().observe(viewLifecycleOwner) {
            if (it) {
                onStep(STEP_REBOOT_STATION)
            }
        }

        model.onBLEConnection().observe(viewLifecycleOwner) {
            if (it) {
                onStep(STEP_CLAIM_STATION)
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
            hideButtons()
            binding.retry.setOnClickListener {
                uiError.retryFunction?.invoke()
                onLoadingState()
            }
            binding.failureButtonsContainer.visible(true)
            binding.steps.visible(false)
            binding.status.clear()
                .animation(R.raw.anim_error)
                .title(R.string.error_claim_failed_title)

            uiError.errorCode?.let {
                binding.status
                    .htmlSubtitle(uiError.errorMessage, it) { navigator.openSupportCenter(context) }
                    .action(resources.getString(R.string.contact_support_title))
                    .listener { navigator.openSupportCenter(context) }
            } ?: binding.status.subtitle(uiError.errorMessage)
        }

        parentModel.onClaimResult().observe(viewLifecycleOwner) {
            updateUI(it)
        }

        onLoadingState()
    }

    private fun onStep(currentStep: Int) {
        if (!binding.steps.isVisible) {
            hideButtons()
            binding.steps.visible(true)
        }
        when (currentStep) {
            STEP_SET_UP_STATION -> binding.firstStep.typeface = Typeface.DEFAULT_BOLD
            STEP_REBOOT_STATION -> {
                binding.firstStep.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_checkmark, 0, 0, 0
                )
                TextViewCompat.setCompoundDrawableTintList(
                    binding.secondStep,
                    ColorStateList.valueOf(requireContext().getColor(R.color.colorOnSurface))
                )
                binding.firstStep.typeface = Typeface.DEFAULT
                binding.secondStep.typeface = Typeface.DEFAULT_BOLD
            }
            STEP_CLAIM_STATION -> {
                binding.secondStep.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_checkmark, 0, 0, 0
                )
                TextViewCompat.setCompoundDrawableTintList(
                    binding.thirdStep,
                    ColorStateList.valueOf(requireContext().getColor(R.color.colorOnSurface))
                )
                binding.secondStep.typeface = Typeface.DEFAULT
                binding.thirdStep.typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    private fun updateUI(resource: Resource<UIDevice>) {
        when (resource.status) {
            Status.SUCCESS -> {
                val device = resource.data
                hideButtons()
                if (device != null && device.isHelium() && device.shouldPromptUpdate()) {
                    analytics.trackEventPrompt(
                        AnalyticsService.ParamValue.OTA_AVAILABLE.paramValue,
                        AnalyticsService.ParamValue.WARN.paramValue,
                        AnalyticsService.ParamValue.VIEW.paramValue
                    )
                    binding.updateBtn.setOnClickListener {
                        onUpdate(device)
                    }
                    initPhotoVerificationBtn(device)
                    binding.skipAndGoToStationBtn.setOnClickListener {
                        showConfirmBypassOTADialog(device)
                    }
                    binding.infoMessage.setHtml(R.string.update_prompt_on_claiming_flow)
                    binding.informationCard.visible(true)
                } else if (device != null) {
                    initPhotoVerificationBtn(device)
                    binding.skipAndGoToStationBtn.setOnClickListener {
                        ActionDialogFragment.createSkipPhotoVerification(requireContext()) {
                            onViewDevice(device)
                        }.show(this)
                    }
                }
                binding.steps.visible(false)
                binding.status.clear()
                    .animation(R.raw.anim_success, false)
                    .title(R.string.station_claimed)
                    .htmlSubtitle(getString(R.string.success_claim_device, device?.name))
                binding.successButtonsContainer.visible(true)
                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
                    contentId = AnalyticsService.ParamValue.CLAIMING_RESULT_ID.paramValue,
                    success = 1L
                )
            }
            Status.ERROR -> {
                hideButtons()
                binding.steps.visible(false)
                binding.retry.setOnClickListener {
                    analytics.trackEventUserAction(
                        actionName = AnalyticsService.ParamValue.CLAIMING_RESULT.paramValue,
                        contentType = AnalyticsService.ParamValue.CLAIMING.paramValue,
                        Pair(
                            AnalyticsService.CustomParam.ACTION.paramName,
                            AnalyticsService.ParamValue.RETRY.paramValue
                        )
                    )
                    onLoadingState()
                    onStep(STEP_CLAIM_STATION)
                    parentModel.claimDevice(locationModel.getInstallationLocation())
                }
                binding.failureButtonsContainer.visible(true)
                binding.status.clear()
                    .animation(R.raw.anim_error)
                    .title(R.string.error_claim_failed_title)
                    .subtitle(resource.message)
                    .action(resources.getString(R.string.contact_support_title))
                    .listener { navigator.openSupportCenter(context) }
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

    private fun onLoadingState() {
        binding.status.clear()
            .animation(R.raw.anim_loading)
            .title(R.string.claiming_station)
            .htmlSubtitle(R.string.claiming_station_helium_desc)
            .show()
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
        activity?.setResult(Activity.RESULT_OK)
        activity?.finish()
    }

    private fun onUpdate(device: UIDevice) {
        analytics.trackEventPrompt(
            AnalyticsService.ParamValue.OTA_AVAILABLE.paramValue,
            AnalyticsService.ParamValue.WARN.paramValue,
            AnalyticsService.ParamValue.ACTION.paramValue
        )
        navigator.showDeviceHeliumOTA(
            context,
            device,
            deviceIsBleConnected = true,
            needsPhotoVerification = true
        )
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

    private fun hideButtons() {
        binding.failureButtonsContainer.invisible()
        binding.successButtonsContainer.invisible()
    }

    private fun initPhotoVerificationBtn(device: UIDevice) {
        binding.photoVerificationBtn.setOnClickListener {
            navigator.showPhotoVerificationIntro(context, device)
            activity?.setResult(Activity.RESULT_OK)
            activity?.finish()
        }
    }
}
