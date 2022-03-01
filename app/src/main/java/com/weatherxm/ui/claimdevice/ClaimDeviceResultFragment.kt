package com.weatherxm.ui.claimdevice

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
import com.weatherxm.databinding.FragmentClaimDeviceResultBinding
import com.weatherxm.ui.Navigator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClaimDeviceResultFragment : Fragment(), KoinComponent {
    private val model: ClaimDeviceViewModel by activityViewModels()
    private lateinit var binding: FragmentClaimDeviceResultBinding
    private val navigator: Navigator by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimDeviceResultBinding.inflate(inflater, container, false)

        model.onClaimResult().observe(viewLifecycleOwner) {
            updateUI(it)
        }

        binding.contactSupport.setOnClickListener {
            navigator.sendSupportEmail(
                context = context,
                subject = getString(R.string.support_email_subject_cannot_claim),
                body = getString(
                    R.string.support_email_body_user_and_device_info,
                    model.getUserEmail(),
                    model.getSerialNumber()
                )
            )
        }

        binding.done.setOnClickListener {
            activity?.setResult(Activity.RESULT_OK)
            activity?.finish()
        }

        return binding.root
    }

    private fun updateUI(resource: Resource<String>) {
        when (resource.status) {
            Status.SUCCESS -> {
                binding.statusView.animation(R.raw.anim_success, false)
                binding.statusView.title(getString(R.string.success))
                binding.statusView.subtitle(resource.data)
                binding.statusView.visibility = View.VISIBLE
                binding.contactSupport.visibility = View.INVISIBLE
                binding.done.isEnabled = true
            }
            Status.ERROR -> {
                binding.statusView.animation(R.raw.anim_error, false)
                binding.statusView.title(getString(R.string.oops_something_wrong))
                binding.statusView.subtitle(resource.message)
                binding.statusView.action(getString(R.string.action_retry))
                binding.statusView.listener { model.claimDevice() }
                binding.statusView.visibility = View.VISIBLE
                binding.contactSupport.visibility = View.VISIBLE
                binding.done.isEnabled = false
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
