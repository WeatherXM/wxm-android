package com.weatherxm.ui.claimdevice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimDeviceBySerialBinding
import com.weatherxm.util.Validator
import com.weatherxm.util.onTextChanged
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

        binding.serialNumber.onTextChanged { serial ->
            binding.serialNumberContainer.error = null
            binding.next.isEnabled = serial.isNotEmpty()
        }

        binding.serialNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.next.performClick()
            }
            true
        }

        binding.next.setOnClickListener {
            val serialNumber = binding.serialNumber.text.toString()

            if (!validator.validateSerialNumber(serialNumber)) {
                binding.serialNumberContainer.error = getString(R.string.invalid_serial_number)
                return@setOnClickListener
            }

            model.setSerialNumber(serialNumber)
            model.next()
        }

        return binding.root
    }
}
