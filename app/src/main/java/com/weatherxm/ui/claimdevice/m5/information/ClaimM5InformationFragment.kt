package com.weatherxm.ui.claimdevice.m5.information

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimM5InstructionsBinding
import com.weatherxm.ui.claimdevice.m5.ClaimM5ViewModel
import com.weatherxm.util.setHtml

class ClaimM5InformationFragment : Fragment() {
    private val parentModel: ClaimM5ViewModel by activityViewModels()
    private lateinit var binding: FragmentClaimM5InstructionsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimM5InstructionsBinding.inflate(inflater, container, false)

        binding.infoText.setHtml(R.string.claim_device_info_text)

        binding.nextButton.setOnClickListener {
            parentModel.next()
        }

        return binding.root
    }
}
