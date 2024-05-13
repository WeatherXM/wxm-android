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
import com.weatherxm.util.Validator
import org.koin.android.ext.android.inject

class FriendlyNameDialogFragment(
    private val currentFriendlyName: String?,
    private val deviceId: String,
    private val resultCallback: (String?) -> Unit
) : DialogFragment() {
    private val analytics: AnalyticsWrapper by inject()
    private lateinit var binding: ViewEditNameBinding

    companion object {
        const val TAG = "FRIENDLY_NAME_DIALOG"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())

        binding = ViewEditNameBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        binding.newName.setText(currentFriendlyName)

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
            resultCallback(null)
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
                resultCallback(newName)
                dismiss()
            }
        }

        return builder.create()
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
