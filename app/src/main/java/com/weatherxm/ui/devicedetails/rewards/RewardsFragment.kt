package com.weatherxm.ui.devicedetails.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentDeviceDetailsRewardsBinding
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
import com.weatherxm.util.Analytics
import com.weatherxm.util.Rewards.formatTokens
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RewardsFragment : BaseFragment() {
    private lateinit var binding: FragmentDeviceDetailsRewardsBinding
    private val parentModel: DeviceDetailsViewModel by activityViewModel()
    private val model: RewardsViewModel by viewModel {
        parametersOf(parentModel.device)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeviceDetailsRewardsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parentModel.onFollowStatus().observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                model.device = parentModel.device
                model.fetchRewardsFromNetwork()
            }
        }

        model.onRewards().observe(viewLifecycleOwner) {
            binding.dailyRewardsCard.updateUI(it.latest) {
                navigator.showRewardDetails(requireContext(), model.device, it.latest)
            }
            val totalRewards = it.allTimeRewards ?: 0F
            binding.totalRewards.text =
                getString(R.string.wxm_amount, formatTokens(totalRewards.toBigDecimal()))
            binding.dailyRewardsCard.setVisible(true)
            binding.totalCard.setVisible(true)
        }

        model.onLoading().observe(viewLifecycleOwner) {
            if (it && binding.swiperefresh.isRefreshing) {
                binding.progress.visibility = View.INVISIBLE
            } else if (it) {
                binding.totalCard.setVisible(false)
                binding.dailyRewardsCard.setVisible(false)
                binding.progress.visibility = View.VISIBLE
            } else {
                binding.swiperefresh.isRefreshing = false
                binding.progress.visibility = View.INVISIBLE
            }
        }

        model.onError().observe(viewLifecycleOwner) {
            binding.totalCard.setVisible(false)
            binding.dailyRewardsCard.setVisible(false)
            showSnackbarMessage(binding.root, it.errorMessage, it.retryFunction)
        }

        binding.swiperefresh.setOnRefreshListener {
            model.fetchRewardsFromNetwork()
        }

        binding.viewTimeline.setOnClickListener {
            navigator.showRewardsList(requireContext(), model.device)
        }

        model.fetchRewardsFromNetwork()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Analytics.Screen.DEVICE_REWARDS, RewardsFragment::class.simpleName)
    }
}
