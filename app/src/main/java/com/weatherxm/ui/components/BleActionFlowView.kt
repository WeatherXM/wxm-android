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
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.show
import com.weatherxm.ui.common.visible

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
                        binding.secondStep.visible(true)
                    }
                    getString(R.styleable.BleActionFlowView_ble_action_flow_third_step)?.let {
                        binding.thirdStep.text = it
                        binding.thirdStep.visible(true)
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
        onCancelButtonClicked: () -> Unit,
        onRetryButtonClicked: () -> Unit
    ) {
        binding.scanAgain.setOnClickListener {
            onScanClicked.invoke()
        }
        binding.pairDevice.setOnClickListener {
            onPairClicked.invoke()
        }
        binding.successButton.setOnClickListener {
            onSuccessPrimaryButtonClicked()
        }
        binding.cancel.setOnClickListener {
            onCancelButtonClicked.invoke()
        }
        binding.retry.setOnClickListener {
            onRetryButtonClicked()
        }
    }

    fun onNotPaired() {
        hideButtons()
        binding.stationName.visible(false)
        binding.firmwareVersionTitle.visible(false)
        binding.firmwareVersions.visible(false)
        binding.steps.visible(false)
        binding.status.visible(false)
        binding.notPairedInfoContainer.visible(true)
        binding.pairDevice.visible(true)
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
        binding.steps.visible(false)
        binding.status.clear()
            .animation(R.raw.anim_error)
            .title(title)

        if (isScanOrBleError) {
            binding.status.subtitle(message)
            binding.scanAgain.visible(true)
        } else {
            binding.retry.text = retryActionText
            binding.failureButtonsContainer.visible(true)
            binding.status
                .htmlSubtitle(message, errorCode) { onErrorAction?.invoke() }
                .action(resources.getString(R.string.contact_support_title))
                .listener { onErrorAction?.invoke() }
        }
    }

    @Suppress("LongParameterList")
    fun onSuccess(@StringRes title: Int, message: String, successActionText: String?) {
        hideButtons()
        binding.steps.visible(false)
        binding.status.clear()
            .animation(R.raw.anim_success, false)
            .title(title)
            .subtitle(message)
        binding.successButton.text = successActionText
        binding.successButton.visible(!successActionText.isNullOrEmpty())
    }

    fun onStep(
        currentStep: Int,
        @StringRes title: Int,
        @StringRes message: Int? = null,
        showProgressBar: Boolean = false,
        showFirmwareInfo: Boolean = false
    ) {
        if (!binding.steps.isVisible) {
            hideButtons()
            binding.notPairedInfoContainer.visible(false)
            binding.installationProgressBar.visible(false)
            binding.stationName.visible(true)
            binding.steps.visible(true)
            binding.status.clear()
                .animation(R.raw.anim_loading)
                .show()
        }
        if (showFirmwareInfo) {
            binding.firmwareVersions.visible(true)
            binding.firmwareVersionTitle.visible(true)
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
            binding.installationProgressBar.visible(true)
        }
    }

    fun onProgressChanged(progress: Int) {
        binding.installationProgressBar.progress = progress
    }

    fun onShowStationUpdateMetadata(stationName: String, firmwareVersions: String) {
        binding.stationName.text = stationName
        binding.firmwareVersions.text = firmwareVersions
        binding.stationName.visible(true)
        binding.firmwareVersionTitle.visible(true)
        binding.firmwareVersions.visible(true)
    }

    private fun hideButtons() {
        binding.failureButtonsContainer.invisible()
        binding.successButton.invisible()
        binding.scanAgain.invisible()
        binding.pairDevice.invisible()
    }
}
