package com.weatherxm.ui.claimdevice.helium.frequency

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.FragmentClaimHeliumFrequencyBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
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

        binding.setFrequencyView.listener(
            onFrequencyDocumentation = {
                analytics.trackEventSelectContent(
                    AnalyticsService.ParamValue.DOCUMENTATION_FREQUENCY.paramValue
                )
                navigator.openWebsite(context, it)
            },
            onBack = {
                // Do nothing. Not applicable.
            },
            onSet = {
                parentModel.setFrequency(
                    model.getFrequency(it)
                )
                parentModel.next()
            })

        model.onFrequencyState().observe(viewLifecycleOwner) { result ->
            binding.setFrequencyView.defaultState(result, true)
        }
    }
}
