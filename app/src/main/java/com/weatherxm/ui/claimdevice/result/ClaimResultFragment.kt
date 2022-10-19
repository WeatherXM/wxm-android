package com.weatherxm.ui.claimdevice.result

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentClaimResultBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.helium.verify.ClaimHeliumVerifyViewModel
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.m5.ClaimM5ViewModel
import com.weatherxm.ui.claimdevice.m5.verify.ClaimM5VerifyViewModel
import com.weatherxm.ui.common.DeviceType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClaimResultFragment : Fragment(), KoinComponent {
    private val m5ParentModel: ClaimM5ViewModel by activityViewModels()
    private val verifyM5Model: ClaimM5VerifyViewModel by activityViewModels()
    private val heliumParentModel: ClaimHeliumViewModel by activityViewModels()
    private val verifyHeliumModel: ClaimHeliumVerifyViewModel by activityViewModels()
    private val locationModel: ClaimLocationViewModel by activityViewModels()
    private lateinit var binding: FragmentClaimResultBinding
    private val navigator: Navigator by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (locationModel.getDeviceType() == DeviceType.M5_WIFI) {
            setupM5ResultScreen()
        } else {
            setupHeliumResultScreen()
        }

        binding.done.setOnClickListener {
            activity?.setResult(Activity.RESULT_OK)
            activity?.finish()
        }
    }

    private fun setupM5ResultScreen() {
        m5ParentModel.onClaimResult().observe(viewLifecycleOwner) {
            updateUI(it)
        }

        binding.contactSupport.setOnClickListener {
            navigator.sendSupportEmail(
                context = context,
                subject = getString(R.string.support_email_subject_cannot_claim),
                body = getString(
                    R.string.support_email_body_user_and_m5_device_info,
                    m5ParentModel.getUserEmail(),
                    verifyM5Model.getSerialNumber()
                )
            )
        }
    }

    private fun setupHeliumResultScreen() {
        heliumParentModel.onClaimResult().observe(viewLifecycleOwner) {
            updateUI(it)
        }

        binding.contactSupport.setOnClickListener {
            navigator.sendSupportEmail(
                context = context,
                subject = getString(R.string.support_email_subject_cannot_claim),
                body = getString(
                    R.string.support_email_body_user_and_helium_device_info,
                    heliumParentModel.getUserEmail(),
                    verifyHeliumModel.getDevEUI(),
                    verifyHeliumModel.getDeviceKey()
                )
            )
        }
    }

    private fun updateUI(resource: Resource<String>) {
        when (resource.status) {
            Status.SUCCESS -> {
                binding.statusView.animation(R.raw.anim_success, false)
                binding.statusView.title(getString(R.string.success))
                binding.statusView.htmlSubtitle(R.string.success_claim_device, resource.data)
                binding.statusView.visibility = View.VISIBLE
                binding.contactSupport.visibility = View.INVISIBLE
                binding.done.isEnabled = true
            }
            Status.ERROR -> {
                binding.statusView.animation(R.raw.anim_error, false)
                binding.statusView.title(getString(R.string.error_generic_message))
                binding.statusView.subtitle(resource.message)
                binding.statusView.action(getString(R.string.action_retry))
                binding.statusView.listener {
                    if (locationModel.getDeviceType() == DeviceType.M5_WIFI) {
                        m5ParentModel.claimDevice(
                            verifyM5Model.getSerialNumber(),
                            locationModel.getInstallationLocation()
                        )
                    } else {
                        heliumParentModel.claimDevice(
                            verifyHeliumModel.getDevEUI(),
                            verifyHeliumModel.getDeviceKey(),
                            locationModel.getInstallationLocation()
                        )
                    }
                }
                binding.statusView.visibility = View.VISIBLE
                binding.contactSupport.visibility = View.VISIBLE
                binding.done.isEnabled = true
            }
            Status.LOADING -> {
                binding.statusView.clear()
                binding.statusView.animation(R.raw.anim_loading)
                binding.statusView.visibility = View.VISIBLE
                binding.contactSupport.visibility = View.INVISIBLE
                binding.done.isEnabled = false
            }
        }
    }
}
