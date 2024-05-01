package com.weatherxm.ui.claimdevice.helium.frequency

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.weatherxm.R
import com.weatherxm.analytics.Analytics
import com.weatherxm.databinding.FragmentClaimHeliumFrequencyBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.components.BaseFragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimHeliumFrequencyFragment : BaseFragment() {
    private val parentModel: ClaimHeliumViewModel by activityViewModel()
    private val model: ClaimHeliumFrequencyViewModel by activityViewModel()
    private lateinit var binding: FragmentClaimHeliumFrequencyBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimHeliumFrequencyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.description) {
            movementMethod =
                me.saket.bettermovementmethod.BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, url ->
                        analytics.trackEventSelectContent(
                            Analytics.ParamValue.DOCUMENTATION_FREQUENCY.paramValue
                        )
                        navigator.openWebsite(context, url)
                        return@setOnLinkClickListener true
                    }
                }
            setHtml(R.string.set_frequency_desc, getString(R.string.helium_frequencies_mapping_url))
        }

        binding.confirmFrequencyToggle.setOnCheckedChangeListener { _, checked ->
            binding.setAndClaimButton.isEnabled = checked
        }

        binding.backButton.setOnClickListener {
            parentModel.backToLocation()
        }

        binding.setAndClaimButton.setOnClickListener {
            parentModel.setFrequency(
                model.getFrequency(binding.frequenciesSelector.selectedItemPosition)
            )
            parentModel.next()
        }

        model.onFrequencyState().observe(viewLifecycleOwner) { result ->
            if (result.country.isNullOrEmpty()) {
                binding.frequencySelectedText.visibility = View.GONE
            } else {
                binding.frequencySelectedText.text = getString(
                    R.string.frequency_selected_text, result.country
                )
            }

            binding.frequenciesSelector.adapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, result.frequencies
            )
        }
    }
}
