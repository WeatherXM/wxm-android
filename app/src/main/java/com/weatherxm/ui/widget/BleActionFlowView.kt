package com.weatherxm.ui.widget

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.weatherxm.R
import com.weatherxm.databinding.ViewBleActionFlowBinding
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.show
import com.weatherxm.util.setHtml

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
                        binding.secondStep.show(null)
                    }
                    getString(R.styleable.BleActionFlowView_ble_action_flow_third_step)?.let {
                        binding.thirdStep.text = it
                        binding.thirdStep.show(null)
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
        binding.steps.visibility = View.GONE
        binding.status.setVisible(false)
        binding.notPairedInfoContainer.visibility = View.VISIBLE
        binding.pairDevice.visibility = View.VISIBLE
    }

    @Suppress("LongParameterList")
    fun onError(
        isScanOrBleError: Boolean,
        @StringRes title: Int,
        retryActionText: String? = null,
        message: String? = null,
        errorCode: String? = null,
        onErrorAction: ((String?) -> Unit)? = null
    ) {
        hideButtons()
        binding.steps.visibility = View.GONE
        binding.status.clear()
        binding.status.animation(R.raw.anim_error)
        binding.status.title(title)

        if (isScanOrBleError) {
            binding.status.subtitle(message)
            binding.scanAgain.visibility = View.VISIBLE
        } else {
            binding.retry.text = retryActionText
            binding.failureButtonsContainer.visibility = View.VISIBLE
            binding.status.htmlSubtitle(message, errorCode) {
                onErrorAction?.invoke(errorCode)
            }
            binding.status.action(resources.getString(R.string.title_contact_support))
            binding.status.listener {
                onErrorAction?.invoke(errorCode)
            }
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
        binding.steps.visibility = View.GONE
        binding.status.clear()
        binding.status.animation(R.raw.anim_success, false)
        binding.status.title(title)
        if (htmlMessage != null) {
            binding.status.htmlSubtitle(htmlMessage, argForHtmlMessage)
        } else {
            binding.status.subtitle(message)
        }
        if (secondaryActionText != null) {
            binding.successOneButtonOnly.visibility = View.GONE
            binding.successPrimaryAction.text = primaryActionText
            binding.successSecondaryAction.text = secondaryActionText
            binding.successPrimaryAction.visibility = View.VISIBLE
            binding.successSecondaryAction.visibility = View.VISIBLE
        } else {
            binding.successOneButtonOnly.text = primaryActionText
            binding.successOneButtonOnly.visibility = View.VISIBLE
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
            binding.notPairedInfoContainer.visibility = View.GONE
            binding.installationProgressBar.visibility = View.GONE
            binding.steps.visibility = View.VISIBLE
            binding.status.clear()
            binding.status.animation(R.raw.anim_loading)
            binding.status.show()
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
                binding.secondStep.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_two_filled, 0, 0, 0
                )
                binding.firstStep.typeface = Typeface.DEFAULT
                binding.secondStep.typeface = Typeface.DEFAULT_BOLD
            }
            2 -> {
                binding.secondStep.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_checkmark, 0, 0, 0
                )
                binding.thirdStep.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_three_filled, 0, 0, 0
                )
                binding.secondStep.typeface = Typeface.DEFAULT
                binding.thirdStep.typeface = Typeface.DEFAULT_BOLD
            }
        }
        if (showProgressBar) {
            binding.installationProgressBar.visibility = View.VISIBLE
        }
    }

    fun onProgressChanged(progress: Int) {
        binding.installationProgressBar.progress = progress
    }

    fun onShowInformationCard() {
        binding.informationCard.visibility = View.VISIBLE
    }

    fun onShowStationUpdateMetadata(stationName: String, firmwareVersions: String) {
        binding.stationName.text = stationName
        binding.firmwareVersions.text = firmwareVersions
        binding.stationName.visibility = View.VISIBLE
        binding.firmwareVersionTitle.visibility = View.VISIBLE
        binding.firmwareVersions.visibility = View.VISIBLE
    }

    private fun hideButtons() {
        binding.successOneButtonOnly.visibility = View.INVISIBLE
        binding.successPrimaryAction.visibility = View.GONE
        binding.successSecondaryAction.visibility = View.GONE
        binding.failureButtonsContainer.visibility = View.INVISIBLE
        binding.scanAgain.visibility = View.INVISIBLE
        binding.pairDevice.visibility = View.INVISIBLE
    }
}
