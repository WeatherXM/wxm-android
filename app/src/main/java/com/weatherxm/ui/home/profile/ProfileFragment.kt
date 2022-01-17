package com.weatherxm.ui.home.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.data.User
import com.weatherxm.databinding.FragmentProfileBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.toast
import com.weatherxm.util.applyTopInset
import org.koin.android.ext.android.inject
import timber.log.Timber

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val model: ProfileViewModel by activityViewModels()
    private val navigator: Navigator by inject()

    private var user: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        binding.root.applyTopInset()

        binding.connectWallet.setOnClickListener {
            navigator.showConnectWallet(this, user?.wallet)
        }

        binding.settings.setOnClickListener {
            navigator.showPreferences(this)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.user().observe(viewLifecycleOwner, { resource ->
            Timber.d("Data updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    updateUI(resource.data, false)
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    resource.message?.let { toast(it) }
                    updateUI(null, false)
                }
                Status.LOADING -> {
                    updateUI(null, true)
                }
            }
        })

        // Fetch user's data
        model.fetch()
    }

    private fun updateUI(user: User?, showProgressBar: Boolean) {
        user?.let {
            if (!it.name.isNullOrEmpty()) {
                binding.nameOrEmail.text = it.name
                binding.nameOrEmail.visibility = View.VISIBLE
            } else if (!it.firstName.isNullOrEmpty() && !it.lastName.isNullOrEmpty()) {
                val nameAndLastname = "${it.firstName} ${it.lastName}"
                binding.nameOrEmail.text = nameAndLastname
                binding.nameOrEmail.visibility = View.VISIBLE
            } else {
                binding.nameOrEmail.visibility = View.GONE
            }

            if (!it.wallet?.address.isNullOrEmpty()) {
                binding.connectWallet.text = getString(R.string.title_change_wallet)
            } else {
                binding.connectWallet.text = getString(R.string.title_connect_wallet)
            }
            this.user = user
        }

        if (showProgressBar) {
            binding.progress.visibility = View.VISIBLE
        } else {
            binding.progress.visibility = View.GONE
        }

    }
}
