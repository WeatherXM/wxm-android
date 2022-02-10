package com.weatherxm.ui.claimdevice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.weatherxm.databinding.FragmentClaimDeviceSetLocationBinding

class ClaimDeviceLocationFragment : Fragment() {
    private val model: ClaimDeviceViewModel by activityViewModels()
    private lateinit var binding: FragmentClaimDeviceSetLocationBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimDeviceSetLocationBinding.inflate(inflater, container, false)

        binding.locationCheckbox.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                model.setLocationInvoke()
                binding.claim.isEnabled = true
            } else {
                binding.claim.isEnabled = false
            }
        }

        binding.claim.setOnClickListener {
            model.next()
            model.claimDevice()
        }

        return binding.root
    }
}
