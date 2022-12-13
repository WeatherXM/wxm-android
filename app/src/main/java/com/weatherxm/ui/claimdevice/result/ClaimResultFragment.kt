package com.weatherxm.ui.claimdevice.result

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentClaimResultBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.location.ClaimLocationViewModel
import com.weatherxm.ui.claimdevice.m5.ClaimM5ViewModel
import com.weatherxm.ui.claimdevice.m5.verify.ClaimM5VerifyViewModel
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.DeviceType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class ClaimResultFragment : Fragment(), KoinComponent {
    private val model: ClaimResultViewModel by viewModels()
    private val m5ParentModel: ClaimM5ViewModel by activityViewModels()
    private val verifyM5Model: ClaimM5VerifyViewModel by activityViewModels()
    private val heliumParentModel: ClaimHeliumViewModel by activityViewModels()
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
    }

    private fun setupM5ResultScreen() {
        binding.quit.setOnClickListener {
            m5ParentModel.cancel()
        }

        binding.retry.setOnClickListener {
            m5ParentModel.claimDevice(
                verifyM5Model.getSerialNumber(),
                locationModel.getInstallationLocation()
            )
        }

        m5ParentModel.onClaimResult().observe(viewLifecycleOwner) {
            it.data?.let { device ->
                model.setClaimedDevice(device)
            }
            updateUI(it)
            updateResult(it)
        }
    }

    private fun updateResult(result: Resource<Device>) {
        when (result.status) {
            Status.SUCCESS -> {
                activity?.setResult(
                    Activity.RESULT_OK,
                    Intent().putExtra(Contracts.ARG_DEVICE, model.getClaimedDevice())
                )
            }
            else -> {}
        }
    }

    private fun setupHeliumResultScreen() {
        binding.quit.setOnClickListener {
            heliumParentModel.cancel()
        }

        binding.retry.setOnClickListener {
            heliumParentModel.claimDevice(locationModel.getInstallationLocation())
        }

        heliumParentModel.onClaimResult().observe(viewLifecycleOwner) {
            it.data?.let { device ->
                model.setClaimedDevice(device)
            }
            updateUI(it)
            updateResult(it)
        }
    }

    private fun updateUI(resource: Resource<Device>) {
        when (resource.status) {
            Status.SUCCESS -> {
                binding.statusView.animation(R.raw.anim_success, false)
                binding.statusView.title(R.string.station_claimed)
                binding.statusView.htmlSubtitle(
                    R.string.success_claim_device,
                    resource.data?.name
                )
                resource.data?.let { device ->
                    binding.viewStation.setOnClickListener {
                        navigator.showUserDevice(activity, device)
                        activity?.finish()
                    }
                }
                binding.failureButtons.visibility = View.GONE
                binding.viewStation.visibility = View.VISIBLE
            }
            Status.ERROR -> {
                binding.statusView.animation(R.raw.anim_error, false)
                binding.statusView.title(R.string.error_claim_failed_title)
                resource.message?.let {
                    binding.statusView.htmlSubtitle(it) {
                        sendSupportEmail(resource.error?.code)
                    }
                }
                binding.statusView.action(getString(R.string.title_contact_support))
                binding.statusView.listener {
                    sendSupportEmail(resource.error?.code)
                }
                binding.viewStation.visibility = View.GONE
                binding.failureButtons.visibility = View.VISIBLE
            }
            Status.LOADING -> {
                binding.statusView.clear()
                binding.statusView.animation(R.raw.anim_loading)
                binding.statusView.title(R.string.claiming_station)
                if (locationModel.getDeviceType() == DeviceType.HELIUM) {
                    binding.statusView.htmlSubtitle(R.string.claiming_station_helium_desc)
                }
                binding.failureButtons.visibility = View.GONE
                binding.viewStation.visibility = View.GONE
            }
        }
    }

    private fun sendSupportEmail(errorCode: String?) {
        if (locationModel.getDeviceType() == DeviceType.M5_WIFI) {
            navigator.sendSupportEmail(
                context = context,
                subject = getString(R.string.support_email_subject_cannot_claim),
                body = getString(
                    R.string.support_email_body_user_and_m5_device_info,
                    m5ParentModel.getUserEmail(),
                    verifyM5Model.getSerialNumber(),
                    errorCode ?: getString(R.string.unknown)
                )
            )
        } else {
            navigator.sendSupportEmail(
                context = context,
                subject = getString(R.string.support_email_subject_cannot_claim),
                body = getString(
                    R.string.support_email_body_user_and_helium_device_info,
                    heliumParentModel.getUserEmail(),
                    heliumParentModel.getDevEUI(),
                    heliumParentModel.getDeviceKey(),
                    errorCode ?: getString(R.string.unknown)
                )
            )
        }
    }
}
