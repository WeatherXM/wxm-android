package com.weatherxm.ui.devicesettings

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.weatherxm.R
import com.weatherxm.databinding.ViewEditNameBinding
import com.weatherxm.util.Analytics
import com.weatherxm.util.Validator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FriendlyNameDialogFragment(
    private val currentFriendlyName: String?,
    private val deviceId: String,
    private val resultCallback: (String?) -> Unit
) : DialogFragment(), KoinComponent {
    private val validator: Validator by inject()
    private val analytics: Analytics by inject()
    private lateinit var binding: ViewEditNameBinding

    companion object {
        const val TAG = "FRIENDLY_NAME_DIALOG"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())

        binding = ViewEditNameBinding.inflate(layoutInflater)
        builder.setTitle(getString(R.string.edit_name))
        builder.setView(binding.root)

        binding.newName.setText(currentFriendlyName)

        binding.cancel.setOnClickListener {
            analytics.trackEventUserAction(
                actionName = Analytics.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                contentType = Analytics.ParamValue.CHANGE_STATION_NAME.paramValue,
                Pair(Analytics.CustomParam.ACTION.paramName, Analytics.ParamValue.CANCEL.paramValue)
            )
            dismiss()
        }

        binding.clear.setOnClickListener {
            analytics.trackEventUserAction(
                actionName = Analytics.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                contentType = Analytics.ParamValue.CHANGE_STATION_NAME.paramValue,
                Pair(Analytics.CustomParam.ACTION.paramName, Analytics.ParamValue.CLEAR.paramValue)
            )
            resultCallback(null)
            dismiss()
        }

        binding.save.setOnClickListener {
            val newName = binding.newName.text.toString().trim()
            if (!validator.validateFriendlyName(newName)) {
                binding.newNameContainer.error =
                    getString(R.string.warn_validation_invalid_friendly_name)
            } else {
                analytics.trackEventUserAction(
                    actionName = Analytics.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                    contentType = Analytics.ParamValue.CHANGE_STATION_NAME.paramValue,
                    Pair(
                        Analytics.CustomParam.ACTION.paramName,
                        Analytics.ParamValue.EDIT.paramValue
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
            Analytics.Screen.CHANGE_STATION_NAME,
            FriendlyNameDialogFragment::class.simpleName,
            deviceId
        )
    }

    fun show(activity: FragmentActivity) {
        show(activity.supportFragmentManager, TAG)
    }
}