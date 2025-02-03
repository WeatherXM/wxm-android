package com.weatherxm.ui.devicesettings

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.databinding.ViewEditNameBinding
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.onTextChanged
import com.weatherxm.ui.components.TermsDialogFragment
import com.weatherxm.util.Validator
import org.koin.android.ext.android.inject

class FriendlyNameDialogFragment() : DialogFragment() {
    private val analytics: AnalyticsWrapper by inject()
    private lateinit var binding: ViewEditNameBinding

    private var resultCallback: ((String?) -> Unit)? = null
    private var currentFriendlyName: String? = null
    private var deviceId: String? = null

    companion object {
        const val TAG = "FRIENDLY_NAME_DIALOG"
        const val AUTO_DISMISS_DIALOG = "AUTO_DISMISS_DIALOG"
    }

    constructor(
        currentFriendlyName: String?,
        deviceId: String,
        resultCallback: (String?) -> Unit
    ) : this() {
        this.currentFriendlyName = currentFriendlyName
        this.deviceId = deviceId
        this.resultCallback = resultCallback
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())

        binding = ViewEditNameBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        binding.newName.setText(currentFriendlyName)

        binding.newName.onTextChanged {
            binding.newNameContainer.error = null
            binding.save.isEnabled = it.isNotEmpty()
        }

        binding.cancel.setOnClickListener {
            analytics.trackEventUserAction(
                actionName = AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                contentType = AnalyticsService.ParamValue.CHANGE_STATION_NAME.paramValue,
                Pair(
                    AnalyticsService.CustomParam.ACTION.paramName,
                    AnalyticsService.ParamValue.CANCEL.paramValue
                )
            )
            dismiss()
        }

        binding.clear.setOnClickListener {
            analytics.trackEventUserAction(
                actionName = AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                contentType = AnalyticsService.ParamValue.CHANGE_STATION_NAME.paramValue,
                Pair(
                    AnalyticsService.CustomParam.ACTION.paramName,
                    AnalyticsService.ParamValue.CLEAR.paramValue
                )
            )
            resultCallback?.invoke(null)
            dismiss()
        }

        binding.save.setOnClickListener {
            val newName = binding.newName.text.toString().trim()
            if (!Validator.validateFriendlyName(newName)) {
                binding.newNameContainer.error =
                    getString(R.string.warn_validation_invalid_friendly_name)
            } else {
                analytics.trackEventUserAction(
                    actionName = AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                    contentType = AnalyticsService.ParamValue.CHANGE_STATION_NAME.paramValue,
                    Pair(
                        AnalyticsService.CustomParam.ACTION.paramName,
                        AnalyticsService.ParamValue.EDIT.paramValue
                    )
                )
                resultCallback?.invoke(newName)
                dismiss()
            }
        }

        /**
         * Enabling "Don't keep activities" causes a crash, and we cannot save the listeners
         * in the savedInstanceState to restore the dialog so we dismiss it automatically
         * and it's the caller's responsibility to re-instantiate it.
         */
        savedInstanceState?.let {
            if (it.getBoolean(AUTO_DISMISS_DIALOG, false)) {
                dismiss()
            }
        }

        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(TermsDialogFragment.AUTO_DISMISS_DIALOG, true)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            AnalyticsService.Screen.CHANGE_STATION_NAME, classSimpleName(), deviceId
        )
    }

    fun show(activity: FragmentActivity) {
        show(activity.supportFragmentManager, TAG)
    }
}
