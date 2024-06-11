package com.weatherxm.ui.claimdevice.pulse.reboot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimPulseRebootBinding
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseViewModel
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.components.BaseFragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimPulseRebootFragment : BaseFragment() {
    private val parentModel: ClaimPulseViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimPulseRebootBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimPulseRebootBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.confirmRebootToggle.setOnCheckedChangeListener { _, checked ->
            binding.nextButton.isEnabled = checked
        }

        binding.nextButton.setOnClickListener {
            parentModel.next()
        }

        binding.firstStep.setHtml(R.string.reboot_gateway_4g_step_1)
        binding.secondStep.setHtml(R.string.reboot_gateway_4g_step_2)
    }
}
