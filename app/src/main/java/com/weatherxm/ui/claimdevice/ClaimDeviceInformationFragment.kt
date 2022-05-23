package com.weatherxm.ui.claimdevice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimDeviceInstructionsBinding
import com.weatherxm.util.setHtml

class ClaimDeviceInformationFragment : Fragment() {
    private lateinit var binding: FragmentClaimDeviceInstructionsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimDeviceInstructionsBinding.inflate(inflater, container, false)

        binding.infoText.setHtml(R.string.claim_device_info_text)

        return binding.root
    }
}
