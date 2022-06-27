package com.weatherxm.ui.claimdevice

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.redmadrobot.inputmask.MaskedTextChangedListener
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimDeviceBySerialBinding
import com.weatherxm.util.setHtml

class ClaimDeviceSerialNumberFragment : Fragment() {
    private val model: ClaimDeviceViewModel by activityViewModels()
    private lateinit var binding: FragmentClaimDeviceBySerialBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimDeviceBySerialBinding.inflate(inflater, container, false)

        binding.instructions.setHtml(R.string.device_serial_number_instructions)

        // Convert typed characters to uppercase
        binding.serialNumber.filters = arrayOf(InputFilter.AllCaps())

        // Mask serial number input
        MaskedTextChangedListener.installOn(
            editText = binding.serialNumber,
            primaryFormat = SERIAL_NUMBER_MASK,
            autocomplete = false,
            autoskip = true,
            valueListener = object : MaskedTextChangedListener.ValueListener {
                override fun onTextChanged(
                    maskFilled: Boolean,
                    extractedValue: String,
                    formattedValue: String
                ) {
                    binding.serialNumberContainer.error = null
                    binding.serialNumberContainer.helperText =
                        "${extractedValue.length}/$SERIAL_NUMBER_MAX_LENGTH"
                    model.nextButtonStatus(
                        extractedValue.isNotEmpty()
                            && extractedValue.length == SERIAL_NUMBER_MAX_LENGTH
                    )
                    if(model.isSerialSet()) {
                        model.setSerialSet(false)
                    }
                }
            })

        binding.serialNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                model.nextButtonClick()
            }
            true
        }

        model.onCheckSerialAndContinue().observe(viewLifecycleOwner) { shouldCheckSerial ->
            if (shouldCheckSerial) {
                if (!model.validateAndSetSerial(binding.serialNumber.text.unmask())) {
                    binding.serialNumberContainer.error =
                        getString(R.string.warn_validation_invalid_serial_number)
                }
            }
        }

        return binding.root
    }

    /**
     * Remove colon ":" character from Serial Number text
     */
    private fun Editable?.unmask(): String = this.toString().replace(":", "")

    companion object {
        const val SERIAL_NUMBER_MASK = "[__]:[__]:[__]:[__]:[__]:[__]:[__]:[__]:[__]"
        const val SERIAL_NUMBER_MAX_LENGTH = 18
    }
}
