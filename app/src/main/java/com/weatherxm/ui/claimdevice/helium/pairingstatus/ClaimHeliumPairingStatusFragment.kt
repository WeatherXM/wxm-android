package com.weatherxm.ui.claimdevice.helium.pairingstatus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentClaimHeliumPairingStatusBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.helium.verify.ClaimHeliumVerifyViewModel
import org.koin.android.ext.android.inject

class ClaimHeliumPairingStatusFragment : BottomSheetDialogFragment() {
    private val parentModel: ClaimHeliumViewModel by activityViewModels()
    private val verifyModel: ClaimHeliumVerifyViewModel by activityViewModels()
    private val model: ClaimHeliumPairingStatusViewModel by viewModels()
    private val navigator: Navigator by inject()
    private lateinit var binding: FragmentClaimHeliumPairingStatusBinding

    companion object {
        const val TAG = "ClaimHeliumPairingStatusFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimHeliumPairingStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun getTheme(): Int {
        return R.style.ThemeOverlay_WeatherXM_BottomSheetDialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.quitClaiming.setOnClickListener {
            parentModel.cancel()
        }

        binding.tryAgain.setOnClickListener {
            model.pair(verifyModel.getDevEUI(), verifyModel.getDeviceKey())
        }

        model.onPairing().observe(viewLifecycleOwner) {
            updateUI(it)
        }

        model.pair(verifyModel.getDevEUI(), verifyModel.getDeviceKey())
    }

    private fun updateUI(result: Resource<String>) {
        when (result.status) {
            Status.SUCCESS -> {
                dismiss()
                parentModel.next()
            }
            Status.ERROR -> {
                binding.buttonsContainer.visibility = View.VISIBLE
                binding.empty.animation(R.raw.anim_error)
                binding.empty.title(R.string.pairing_failed)
                binding.empty.htmlSubtitle(R.string.pairing_failed_desc, result.message) {
                    sendSupportEmail()
                }
                binding.empty.action(getString(R.string.title_contact_support))
                binding.empty.listener {
                    sendSupportEmail()
                }
            }
            Status.LOADING -> {
                binding.buttonsContainer.visibility = View.GONE
                binding.empty.clear()
                binding.empty.animation(R.raw.anim_loading)
                binding.empty.title(R.string.pairing_device)
                binding.empty.htmlSubtitle(R.string.pairing_device_desc)
            }
        }
    }

    private fun sendSupportEmail() {
        navigator.sendSupportEmail(
            context = context,
            subject = getString(R.string.support_email_subject_helium_pairing_failed),
            body = getString(
                R.string.support_email_body_claiming_helium,
                verifyModel.getDevEUI(),
                verifyModel.getDeviceKey()
            )
        )
    }
}
