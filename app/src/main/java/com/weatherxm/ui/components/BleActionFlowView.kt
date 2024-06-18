package com.weatherxm.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.weatherxm.R
import com.weatherxm.databinding.ViewBleActionFlowBinding
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.setInvisible
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.show

@Suppress("TooManyFunctions")
class BleActionFlowView : ConstraintLayout {

    private lateinit var binding: ViewBleActionFlowBinding

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context?, attrs: AttributeSet? = null) {
        binding = ViewBleActionFlowBinding.inflate(LayoutInflater.from(context), this)

        binding.pairFirstStep.setHtml(R.string.reset_ble_first_step)
        binding.pairSecondStep.setHtml(R.string.tap_pair_device_button)

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.BleActionFlowView, 0, 0)
            .apply {
                try {
                    getString(R.styleable.BleActionFlowView_ble_action_flow_first_step)?.let {
                        binding.firstStep.text = it
                    }
                    getString(R.styleable.BleActionFlowView_ble_action_flow_second_step)?.let {
                        binding.secondStep.text = it
                        binding.secondStep.setVisible(true)
                    }
                    getString(R.styleable.BleActionFlowView_ble_action_flow_third_step)?.let {
                        binding.thirdStep.text = it
                        binding.thirdStep.setVisible(true)
                    }
                } finally {
                    recycle()
                }
            }
    }

    @Suppress("LongParameterList")
    fun setListeners(
        onScanClicked: () -> Unit,
        onPairClicked: () -> Unit,
        onSuccessPrimaryButtonClicked: () -> Unit,
        onSuccessSecondaryButtonClicked: (() -> Unit)? = null,
        onCancelButtonClicked: () -> Unit,
        onRetryButtonClicked: () -> Unit
    ) {
        binding.scanAgain.setOnClickListener {
            onScanClicked.invoke()
        }
        binding.pairDevice.setOnClickListener {
            onPairClicked.invoke()
        }
        setSuccessOneButtonOnlyListener(onSuccessPrimaryButtonClicked)
        setSuccessPrimaryButtonListener(onSuccessPrimaryButtonClicked)
        onSuccessSecondaryButtonClicked?.let {
            setSuccessSecondaryButtonListener(it)
        }
        binding.cancel.setOnClickListener {
            onCancelButtonClicked.invoke()
        }
        setRetryButtonListener(onRetryButtonClicked)
    }

    fun setSuccessOneButtonOnlyListener(listener: () -> Unit) {
        binding.successOneButtonOnly.setOnClickListener {
            listener.invoke()
        }
    }

    fun setSuccessPrimaryButtonListener(listener: () -> Unit) {
        binding.successPrimaryAction.setOnClickListener {
            listener.invoke()
        }
    }

    fun setSuccessSecondaryButtonListener(listener: () -> Unit) {
        binding.successSecondaryAction.setOnClickListener {
            listener.invoke()
        }
    }

    fun setRetryButtonListener(listener: (() -> Unit)?) {
        binding.retry.setOnClickListener {
            listener?.invoke()
        }
    }

    fun onNotPaired() {
        hideButtons()
        binding.stationName.setVisible(false)
        binding.firmwareVersionTitle.setVisible(false)
        binding.firmwareVersions.setVisible(false)
        binding.steps.setVisible(false)
        binding.status.setVisible(false)
        binding.notPairedInfoContainer.setVisible(true)
        binding.pairDevice.setVisible(true)
    }

    @Suppress("LongParameterList")
    fun onError(
        isScanOrBleError: Boolean,
        @StringRes title: Int,
        retryActionText: String? = null,
        message: String? = null,
        errorCode: String? = null,
        onErrorAction: (() -> Unit)? = null
    ) {
        hideButtons()
        binding.steps.setVisible(false)
        binding.status.clear()
            .animation(R.raw.anim_error)
            .title(title)

        if (isScanOrBleError) {
            binding.status.subtitle(message)
            binding.scanAgain.setVisible(true)
        } else {
            binding.retry.text = retryActionText
            binding.failureButtonsContainer.setVisible(true)
            binding.status
                .htmlSubtitle(message, errorCode) { onErrorAction?.invoke() }
                .action(resources.getString(R.string.contact_support_title))
                .listener { onErrorAction?.invoke() }
        }
    }

    @Suppress("LongParameterList")
    fun onSuccess(
        @StringRes title: Int,
        message: String? = null,
        htmlMessage: String? = null,
        argForHtmlMessage: String? = null,
        primaryActionText: String,
        secondaryActionText: String? = null
    ) {
        hideButtons()
        binding.steps.setVisible(false)
        binding.status.clear()
            .animation(R.raw.anim_success, false)
            .title(title)
        if (htmlMessage != null) {
            binding.status.htmlSubtitle(htmlMessage, argForHtmlMessage)
        } else {
            binding.status.subtitle(message)
        }
        if (secondaryActionText != null) {
            binding.successOneButtonOnly.setVisible(false)
            binding.successPrimaryAction.text = primaryActionText
            binding.successSecondaryAction.text = secondaryActionText
            binding.successPrimaryAction.setVisible(true)
            binding.successSecondaryAction.setVisible(true)
        } else {
            binding.successOneButtonOnly.text = primaryActionText
            binding.successOneButtonOnly.setVisible(true)
        }
    }

    fun onStep(
        currentStep: Int,
        @StringRes title: Int,
        @StringRes message: Int? = null,
        showProgressBar: Boolean = false
    ) {
        if (!binding.steps.isVisible) {
            hideButtons()
            binding.notPairedInfoContainer.setVisible(false)
            binding.installationProgressBar.setVisible(false)
            binding.stationName.setVisible(true)
            binding.firmwareVersions.setVisible(true)
            binding.firmwareVersionTitle.setVisible(true)
            binding.steps.setVisible(true)
            binding.status.clear()
                .animation(R.raw.anim_loading)
                .show()
        }
        binding.status.title(title)
        if (message != null) {
            binding.status.htmlSubtitle(message)
        } else {
            binding.status.subtitle(null)
        }
        when (currentStep) {
            0 -> {
                binding.firstStep.typeface = Typeface.DEFAULT_BOLD
            }
            1 -> {
                binding.firstStep.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_checkmark, 0, 0, 0
                )
                TextViewCompat.setCompoundDrawableTintList(
                    binding.secondStep,
                    ColorStateList.valueOf(context.getColor(R.color.colorOnSurface))
                )
                binding.firstStep.typeface = Typeface.DEFAULT
                binding.secondStep.typeface = Typeface.DEFAULT_BOLD
            }
            2 -> {
                binding.secondStep.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_checkmark, 0, 0, 0
                )
                TextViewCompat.setCompoundDrawableTintList(
                    binding.thirdStep,
                    ColorStateList.valueOf(context.getColor(R.color.colorOnSurface))
                )
                binding.secondStep.typeface = Typeface.DEFAULT
                binding.thirdStep.typeface = Typeface.DEFAULT_BOLD
            }
        }
        if (showProgressBar) {
            binding.installationProgressBar.setVisible(true)
        }
    }

    fun onProgressChanged(progress: Int) {
        binding.installationProgressBar.progress = progress
    }

    fun onShowInformationCard() {
        binding.informationCard.setVisible(true)
    }

    fun onShowStationUpdateMetadata(stationName: String, firmwareVersions: String) {
        binding.stationName.text = stationName
        binding.firmwareVersions.text = firmwareVersions
        binding.stationName.setVisible(true)
        binding.firmwareVersionTitle.setVisible(true)
        binding.firmwareVersions.setVisible(true)
    }

    private fun hideButtons() {
        binding.successOneButtonOnly.setInvisible()
        binding.successPrimaryAction.setVisible(false)
        binding.successSecondaryAction.setVisible(false)
        binding.failureButtonsContainer.setInvisible()
        binding.scanAgain.setInvisible()
        binding.pairDevice.setInvisible()
    }
}
