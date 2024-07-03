package com.weatherxm.ui.claimdevice.pulse.claimingcode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import coil.ImageLoader
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimPulseClaimingCodeBinding
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseViewModel
import com.weatherxm.ui.common.loadImage
import com.weatherxm.ui.common.onTextChanged
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.components.BaseFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimPulseClaimingCodeFragment : BaseFragment() {
    private val model: ClaimPulseViewModel by activityViewModel()
    private val imageLoader: ImageLoader by inject()
    private lateinit var binding: FragmentClaimPulseClaimingCodeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimPulseClaimingCodeBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.claimingKey.onTextChanged {
            binding.claimingKeyContainer.error = null
            binding.proceedBtn.isEnabled = it.length == CLAIMING_KEY_MAX_LENGTH
        }

        binding.claimingKey.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validateAndSetClaimingKey()
            }
            true
        }

        binding.proceedBtn.setOnClickListener {
            validateAndSetClaimingKey()
        }

        binding.secondStep.setHtml(R.string.type_gateway_claiming_key_step_2)
        binding.guideGif.loadImage(imageLoader, R.raw.pulse_claiming_key)
    }

    private fun validateAndSetClaimingKey() {
        val claimingKey = binding.claimingKey.text.toString()
        if (model.validateClaimingKey(claimingKey)) {
            model.setClaimingKey(claimingKey)
            model.next()
        } else {
            binding.claimingKeyContainer.error =
                getString(R.string.warn_validation_invalid_claiming_key)
        }
    }

    companion object {
        const val CLAIMING_KEY_MAX_LENGTH = 6
    }
}
