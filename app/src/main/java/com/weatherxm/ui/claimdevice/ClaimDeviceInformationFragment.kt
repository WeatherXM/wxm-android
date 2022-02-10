package com.weatherxm.ui.claimdevice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimDeviceInstructionsBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.util.setHtml
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClaimDeviceInformationFragment : Fragment(), KoinComponent {
    private val model: ClaimDeviceViewModel by activityViewModels()
    private val navigator: Navigator by inject()
    private lateinit var binding: FragmentClaimDeviceInstructionsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimDeviceInstructionsBinding.inflate(inflater, container, false)

        binding.infoText.setHtml(R.string.claim_device_info_text)

        binding.next.setOnClickListener {
            model.next()
        }

        binding.buyMiner.setOnClickListener {
            navigator.openWebsite(this, "https://weatherxm.com")
        }

        return binding.root
    }
}
