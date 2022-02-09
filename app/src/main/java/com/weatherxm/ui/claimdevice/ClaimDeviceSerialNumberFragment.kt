package com.weatherxm.ui.claimdevice

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.redmadrobot.inputmask.MaskedTextChangedListener
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimDeviceBySerialBinding
import com.weatherxm.util.Validator
import com.weatherxm.util.setHtml
import org.koin.android.ext.android.inject

class ClaimDeviceSerialNumberFragment : Fragment() {
    private val model: ClaimDeviceViewModel by activityViewModels()
    private val validator: Validator by inject()
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
                    binding.next.isEnabled = extractedValue.isNotEmpty()
                }
            })

        binding.serialNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.next.performClick()
            }
            true
        }

        binding.next.setOnClickListener {
            val serialNumber = binding.serialNumber.text.unmask()

            if (!validator.validateSerialNumber(serialNumber)) {
                binding.serialNumberContainer.error = getString(R.string.invalid_serial_number)
                return@setOnClickListener
            }

            model.setSerialNumber(serialNumber)
            model.next()
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
