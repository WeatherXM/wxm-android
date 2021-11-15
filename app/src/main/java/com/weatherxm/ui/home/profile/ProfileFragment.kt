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
import com.weatherxm.util.ResourcesHelper
import dev.chrisbanes.insetter.applyInsetter
import org.koin.android.ext.android.inject
import org.koin.core.component.inject
import timber.log.Timber

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val model: ProfileViewModel by activityViewModels()
    private val navigator: Navigator by inject()
    private val resHelper: ResourcesHelper by inject()

    private var user: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding.root.applyInsetter {
            type(statusBars = true) {
                margin(left = false, top = true, right = false, bottom = false)
            }
        }

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
                    updateUI(resource.data)
                }
                Status.ERROR -> {
                }
                Status.LOADING -> {
                }
            }
        })

        // Fetch user's data
        model.fetch()
    }

    private fun updateUI(user: User?) {
        if (!user?.name.isNullOrEmpty()) {
            binding.nameOrEmail.text = user?.name
            binding.nameOrEmail.visibility = View.VISIBLE
        } else if (!user?.firstName.isNullOrEmpty() && !user?.lastName.isNullOrEmpty()) {
            val nameAndLastname = "${user?.firstName} ${user?.lastName}"
            binding.nameOrEmail.text = nameAndLastname
            binding.nameOrEmail.visibility = View.VISIBLE
        } else {
            binding.nameOrEmail.visibility = View.GONE
        }

        if(!user?.wallet?.address.isNullOrEmpty()) {
            binding.connectWallet.text = resHelper.getString(R.string.title_change_wallet)
        } else {
            binding.connectWallet.text = resHelper.getString(R.string.title_connect_wallet)
        }
        this.user = user
    }
}
