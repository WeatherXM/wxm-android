package com.weatherxm.ui.claimdevice.helium.reset

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimDeviceHeliumResetBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumDeviceViewModel
import com.weatherxm.util.setHtml
import org.koin.android.ext.android.inject

class ClaimHeliumDeviceResetFragment : Fragment() {
    private val parentModel: ClaimHeliumDeviceViewModel by activityViewModels()
    private val navigator: Navigator by inject()
    private lateinit var binding: FragmentClaimDeviceHeliumResetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimDeviceHeliumResetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nextButton.setOnClickListener {
            parentModel.next()
        }

        binding.firstStep.setHtml(R.string.reset_first_step)
        binding.secondStep.setHtml(R.string.reset_second_step)
    }
}
