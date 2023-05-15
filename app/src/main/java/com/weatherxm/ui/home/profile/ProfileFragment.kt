package com.weatherxm.ui.home.profile

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.data.User
import com.weatherxm.databinding.FragmentProfileBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.home.HomeViewModel
import com.weatherxm.util.Analytics
import com.weatherxm.util.applyInsets
import org.koin.android.ext.android.inject
import timber.log.Timber

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val model: ProfileViewModel by viewModels()
    private val parentModel: HomeViewModel by activityViewModels()
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()

    // Register the launcher for the connect wallet activity and wait for a possible result
    private val connectWalletLauncher =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                parentModel.setWalletNotMissing()
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
            navigator.showConnectWallet(connectWalletLauncher, this)
        }

        binding.settings.setOnClickListener {
            navigator.showPreferences(this)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.user().observe(viewLifecycleOwner) { resource ->
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

        // Hide/show badge if user has connected a wallet or not
        parentModel.onWalletMissing().observe(viewLifecycleOwner) {
            binding.connectWalletNotification.visibility = if (it) VISIBLE else GONE
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.PROFILE,
            ProfileFragment::class.simpleName
        )
    }

    private fun updateUI(user: User?, showProgressBar: Boolean) {
        user?.let {
            binding.toolbar.title = if (it.name == it.email) {
                getString(R.string.hello)
            } else {
                getString(R.string.hello_user, it.name)
            }
            if (it.email.isEmpty()) {
                binding.email.visibility = GONE
            } else {
                binding.email.text = it.email
                binding.email.visibility = VISIBLE
            }
        }

        if (showProgressBar) {
            binding.progress.visibility = VISIBLE
        } else {
            binding.progress.visibility = View.INVISIBLE
        }
    }
}
