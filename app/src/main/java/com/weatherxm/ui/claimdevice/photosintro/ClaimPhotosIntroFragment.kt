package com.weatherxm.ui.claimdevice.photosintro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimPhotosIntroBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseViewModel
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiViewModel
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_TYPE
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.photoverification.PhotoExampleAdapter
import com.weatherxm.ui.photoverification.badExamples
import com.weatherxm.ui.photoverification.goodExamples
import com.weatherxm.ui.photoverification.intro.PhotoVerificationIntroViewModel
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ClaimPhotosIntroFragment : BaseFragment() {
    private val heliumParentModel: ClaimHeliumViewModel by activityViewModel()
    private val wifiParentModel: ClaimWifiViewModel by activityViewModel()
    private val pulseParentModel: ClaimPulseViewModel by activityViewModel()
    private val model: PhotoVerificationIntroViewModel by viewModel()
    private lateinit var binding: FragmentClaimPhotosIntroBinding

    companion object {
        fun newInstance(deviceType: DeviceType) = ClaimPhotosIntroFragment().apply {
            arguments = Bundle().apply { putParcelable(ARG_DEVICE_TYPE, deviceType) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimPhotosIntroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deviceType = arguments?.parcelable<DeviceType>(ARG_DEVICE_TYPE)
        if (context == null || deviceType == null) {
            // No point executing if in the meanwhile the activity is dead
            return
        }

        val goodExampleAdapter = PhotoExampleAdapter()
        val badExampleAdapter = PhotoExampleAdapter()
        binding.goodExamplesRecycler.adapter = goodExampleAdapter
        binding.badExamplesRecycler.adapter = badExampleAdapter

        goodExampleAdapter.submitList(goodExamples)
        badExampleAdapter.submitList(badExamples)

        with(binding.acceptableUsePolicyCheckboxDesc) {
            movementMethod = BetterLinkMovementMethod.newInstance().apply {
                setOnLinkClickListener { _, url ->
                    navigator.openWebsite(activity, url)
                    return@setOnLinkClickListener true
                }
            }
            setHtml(
                R.string.accept_acceptable_use_policy,
                getString(R.string.acceptable_use_policy_url)
            )
        }

        binding.acceptableUsePolicyCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.takePhotoBtn.isEnabled = isChecked
        }

        binding.takePhotoBtn.setOnClickListener {
            model.setAcceptedTerms()
            when (deviceType) {
                DeviceType.M5_WIFI, DeviceType.D1_WIFI -> wifiParentModel.next()
                DeviceType.PULSE_4G -> pulseParentModel.next()
                DeviceType.HELIUM -> heliumParentModel.next()
            }
        }
        if (model.getAcceptedTerms()) {
            binding.acceptableUsePolicyCheckbox.isChecked = true
        }
    }
}
