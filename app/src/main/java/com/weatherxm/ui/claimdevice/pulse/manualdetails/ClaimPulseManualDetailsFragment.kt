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
        const val PULSE_SERIAL_MAX_LENGTH = 17
    }
}
