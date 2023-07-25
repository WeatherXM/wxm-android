package com.weatherxm.ui.devicedetails.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.databinding.FragmentDeviceDetailsRewardsBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
import com.weatherxm.util.Analytics
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class RewardsFragment : Fragment(), KoinComponent {
    private lateinit var binding: FragmentDeviceDetailsRewardsBinding
    private val parentModel: DeviceDetailsViewModel by activityViewModels()
    private val model: RewardsViewModel by viewModel {
        parametersOf(parentModel.device, parentModel.cellDevice)
    }
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()
    private var snackbar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeviceDetailsRewardsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.onTokens().observe(viewLifecycleOwner) {
            if (parentModel.isUserDevice) {
                binding.tokenCard.setTokenInfo(
                    it,
                    model.device.rewards?.totalRewards,
                    model.device.id
                )
            } else {
                binding.tokenCard.setTokenInfo(it, null, model.cellDevice.id)
            }
        }

        model.onLoading().observe(viewLifecycleOwner) {
            if (it && binding.swiperefresh.isRefreshing) {
                binding.progress.visibility = View.INVISIBLE
            } else if (it) {
                binding.progress.visibility = View.VISIBLE
            } else {
                binding.swiperefresh.isRefreshing = false
                binding.progress.visibility = View.INVISIBLE
            }
        }

        model.onError().observe(viewLifecycleOwner) {
            showSnackbarMessage(it.errorMessage, it.retryFunction)
        }

        binding.swiperefresh.setOnRefreshListener {
            fetchTokenDetails()
        }

        binding.tokenRewards.setOnClickListener {
            navigator.showTokenScreen(requireContext(), model.device)
        }

        if (!parentModel.isUserDevice) {
            binding.tokenRewards.setVisible(false)
        }

        fetchTokenDetails()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.REWARDS,
            RewardsFragment::class.simpleName
        )
    }

    private fun fetchTokenDetails() {
        if (parentModel.isUserDevice) {
            model.fetchTokenDetails()
        } else {
            model.fetchCellDeviceTokenDetails()
        }
    }

    private fun showSnackbarMessage(message: String, callback: (() -> Unit)? = null) {
        if (snackbar?.isShown == true) {
            snackbar?.dismiss()
        }

        if (callback != null) {
            snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
            snackbar?.setAction(R.string.action_retry) {
                callback()
            }
        } else {
            snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        }
        snackbar?.show()
    }
}
