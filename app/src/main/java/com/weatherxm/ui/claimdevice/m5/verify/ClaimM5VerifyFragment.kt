package com.weatherxm.ui.claimdevice.m5.verify

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.redmadrobot.inputmask.MaskedTextChangedListener
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimM5VerifyBinding
import com.weatherxm.ui.claimdevice.m5.ClaimM5ViewModel
import com.weatherxm.ui.common.unmask

class ClaimM5VerifyFragment : Fragment() {
    private val parentModel: ClaimM5ViewModel by activityViewModels()
    private val model: ClaimM5VerifyViewModel by activityViewModels()
    private lateinit var binding: FragmentClaimM5VerifyBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimM5VerifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                    binding.verify.isEnabled = extractedValue.isNotEmpty()
                        && extractedValue.length == SERIAL_NUMBER_MAX_LENGTH

                    /**
                     * Acts as "resetting" the serial number in case the user goes back and forth
                     * in the serial number & location screen and edits the SN after he has already
                     * set it successfully once
                     */
                    model.setSerialNumber("")
                }
            })

        binding.serialNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                validateAndSetSerial()
            }
            true
        }

        binding.verify.setOnClickListener {
            validateAndSetSerial()
        }
    }

    private fun validateAndSetSerial() {
        if (!model.validateSerial(binding.serialNumber.text.unmask())) {
            binding.serialNumberContainer.error =
                getString(R.string.warn_validation_invalid_serial_number)
        } else {
            model.setSerialNumber(binding.serialNumber.text.unmask())
            parentModel.next()
        }
    }

    companion object {
        const val SERIAL_NUMBER_MASK = "[__]:[__]:[__]:[__]:[__]:[__]:[__]:[__]:[__]"
        const val SERIAL_NUMBER_MAX_LENGTH = 18
    }
}
