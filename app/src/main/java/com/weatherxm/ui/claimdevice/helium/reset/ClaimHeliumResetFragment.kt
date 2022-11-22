package com.weatherxm.ui.claimdevice.helium.reset

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimHeliumResetBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.util.setHtml

class ClaimHeliumResetFragment : Fragment() {
    private val parentModel: ClaimHeliumViewModel by activityViewModels()
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

        binding.nextButton.setOnClickListener {
            parentModel.next()
        }

        binding.firstStep.setHtml(R.string.reset_ble_first_step)

        // TODO: Remove if we do not use again the manual claiming
//        binding.claimManually.setOnClickListener {
//            parentModel.claimManually()
//        }
//
//        if (parentModel.isManualClaiming()) {
//            binding.firstStep.setHtml(R.string.reset_manual_first_step)
//            binding.claimManually.visibility = View.GONE
//        } else {
//            binding.firstStep.setHtml(R.string.reset_ble_first_step)
//        }

        binding.secondStep.setHtml(R.string.reset_second_step)
    }
}
