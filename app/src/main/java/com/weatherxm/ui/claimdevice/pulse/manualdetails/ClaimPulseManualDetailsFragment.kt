package com.weatherxm.ui.claimdevice.pulse.manualdetails

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimPulseManualDetailsBinding
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseViewModel
import com.weatherxm.ui.common.onTextChanged
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.components.BaseFragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimPulseManualDetailsFragment : BaseFragment() {
    private val model: ClaimPulseViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimPulseManualDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimPulseManualDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Convert typed characters to uppercase
        binding.serialNumber.filters = arrayOf(
            InputFilter.AllCaps(), InputFilter.LengthFilter(PULSE_SERIAL_MAX_LENGTH)
        )

        binding.serialNumber.setOnFocusChangeListener { v, hasFocus ->
            /**
             * Hacky way to show hint, as then unfocused, the starting "P" gets overlapped
             * by the prefix/placeholder "P", so it's shown correctly.
             * But when focused, the hint starts after the prefix "P" (so we have two "P" there)
             * so we want to remove the starting "P" from the hint and keep only the prefix.
             *
             * The following cases apply as to when we use "prefix" or "placeholder" due to a bug
             * which a double "PP" is shown in the UI if the camera is opened first and this screen
             * is shown afterwards:
             * 1. hasFocus ✔️ isInputEmpty ✔️ ----> use PREFIX
             * 2. hasFocus ✔️ isInputEmpty ❌ ----> use PREFIX
             * 3. hasFocus ❌ isInputEmpty ✔️ ----> use PLACEHOLDER
             * 4. hasFocus ❌ isInputEmpty ❌ ----> use PREFIX
             */
            if (!hasFocus && binding.serialNumber.text?.isEmpty() == true) {
                binding.serialNumber.hint = getString(R.string.enter_your_gateway_pulse_sn_hint)
                binding.serialNumberContainer.prefixText = null
                binding.serialNumberContainer.placeholderText = "P"
            } else {
                binding.serialNumber.hint =
                    getString(R.string.enter_your_gateway_pulse_sn_hint).removePrefix("P")
                binding.serialNumberContainer.prefixText = "P"
                binding.serialNumberContainer.placeholderText = null
            }
        }

        binding.serialNumber.onTextChanged {
            binding.serialNumberContainer.error = null
            binding.proceedBtn.isEnabled = it.length == PULSE_SERIAL_MAX_LENGTH
        }

        binding.serialNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validateAndSetDetails()
            }
            true
        }

        binding.proceedBtn.setOnClickListener {
            validateAndSetDetails()
        }

        binding.desc.setHtml(R.string.enter_gateway_pulse_serial_number_desc)
    }

    private fun validateAndSetDetails() {
        val serialNumber = binding.serialNumber.text.toString()
        if (model.validateSerial(serialNumber)) {
            model.setSerialNumber(serialNumber)
            model.next()
        } else {
            binding.serialNumberContainer.error = getString(R.string.warn_validation_invalid_serial)
        }
    }

    companion object {
        const val PULSE_SERIAL_MAX_LENGTH = 16
    }
}
