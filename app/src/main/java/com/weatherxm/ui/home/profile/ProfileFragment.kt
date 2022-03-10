package com.weatherxm.ui.home.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentProfileBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.ProfileInfo
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.connectwallet.ConnectWalletActivity
import com.weatherxm.ui.connectwallet.ConnectWalletActivity.Companion.ARG_WALLET
import com.weatherxm.util.applyInsets
import org.koin.android.ext.android.inject
import timber.log.Timber

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val model: ProfileViewModel by activityViewModels()
    private val navigator: Navigator by inject()

    private var profileInfo: ProfileInfo? = null

    // Register the launcher for the connect wallet activity and wait for a possible result
    private val connectWalletLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                model.walletConnected()
                profileInfo?.walletAddress = model.getWalletAddressFromCache()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        binding.root.applyInsets()

        binding.connectWallet.setOnClickListener {
            this.context?.let {
                val intent = Intent(it, ConnectWalletActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(ARG_WALLET, profileInfo?.walletAddress)

                connectWalletLauncher.launch(intent)
            }
        }

        binding.settings.setOnClickListener {
            navigator.showPreferences(this)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.profileInfo().observe(viewLifecycleOwner) { resource ->
            Timber.d("Data updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    updateUI(resource.data, false)
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    resource.message?.let { context.toast(it) }
                    updateUI(null, false)
                }
                Status.LOADING -> {
                    updateUI(null, true)
                }
            }
        }

        // Fetch profile's info
        model.fetchProfileInfo()

        model.hasWallet().observe(viewLifecycleOwner) {
            if (it) {
                binding.connectWalletNotification.visibility = View.GONE
            }
        }
    }

    private fun updateUI(profileInfo: ProfileInfo?, showProgressBar: Boolean) {
        profileInfo?.let {
            binding.toolbar.title = if (it.name == it.email) {
                getString(R.string.hello)
            } else {
                getString(R.string.hello_user, it.name)
            }
            if (it.email.isEmpty()) {
                binding.email.visibility = View.GONE
            } else {
                binding.email.text = it.email
                binding.email.visibility = View.VISIBLE
            }
            this.profileInfo = profileInfo
        }

        if (showProgressBar) {
            binding.progress.visibility = View.VISIBLE
        } else {
            binding.progress.visibility = View.INVISIBLE
        }

        if (profileInfo?.walletAddress.isNullOrEmpty()) {
            binding.connectWalletNotification.visibility = View.VISIBLE
        }
    }
}
