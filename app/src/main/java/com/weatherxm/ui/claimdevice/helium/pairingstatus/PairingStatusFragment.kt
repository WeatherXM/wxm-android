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
import com.weatherxm.databinding.FragmentClaimDeviceHeliumPairingStatusBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumDeviceViewModel
import org.koin.android.ext.android.inject

class PairingStatusFragment : BottomSheetDialogFragment() {
    private val parentModel: ClaimHeliumDeviceViewModel by activityViewModels()
    private val model: PairingStatusViewModel by viewModels()
    private val navigator: Navigator by inject()
    private lateinit var binding: FragmentClaimDeviceHeliumPairingStatusBinding

    companion object {
        const val TAG = "PairingStatusFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimDeviceHeliumPairingStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun getTheme(): Int {
        return R.style.ThemeOverlay_WeatherXM_BottomSheetDialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.closeButton.setOnClickListener {
            // TODO: What else other than dismiss?!
            dismiss()
        }

        binding.quitClaiming.setOnClickListener {
            parentModel.cancel()
        }

        binding.tryAgain.setOnClickListener {
            model.pair()
        }

        model.onPairing().observe(viewLifecycleOwner) {
            updateUI(it)
        }

        model.pair()
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
                parentModel.getDevEUI(),
                parentModel.getDeviceKey()
            )
        )
    }
}
