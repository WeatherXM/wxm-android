package com.weatherxm.ui.claimdevice.helium.reset

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimHeliumResetBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.components.BaseFragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimHeliumResetFragment : BaseFragment() {
    private val parentModel: ClaimHeliumViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimHeliumResetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimHeliumResetBinding.inflate(inflater, container, false)
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

        binding.firstStep.setHtml(R.string.reset_ble_first_step)
    }
}
