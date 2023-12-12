package com.weatherxm.ui.devicedetails.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentDeviceDetailsRewardsBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
import com.weatherxm.util.Analytics
import com.weatherxm.util.Rewards.formatTokens
import com.weatherxm.util.onTabSelected
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class RewardsFragment : Fragment(), KoinComponent {
    private lateinit var binding: FragmentDeviceDetailsRewardsBinding
    private val parentModel: DeviceDetailsViewModel by activityViewModels()
    private val model: RewardsViewModel by viewModel {
        parametersOf(parentModel.device)
    }
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()
    private var snackbar: Snackbar? = null

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
                model.fetchRewardsFromNetwork(
                    RewardsViewModel.TabSelected.entries[binding.selectorGroup.selectedTabPosition]
                )
            }
        }

        model.onRewardsObject().observe(viewLifecycleOwner) {
            binding.rewardsContentCard.updateUI(
                it,
                model.device,
                RewardsViewModel.TabSelected.entries[binding.selectorGroup.selectedTabPosition],
                onInfoButton = { title, htmlMessage ->
                    navigator.showMessageDialog(
                        childFragmentManager, title = title, htmlMessage = htmlMessage
                    )
                }
            ) {
                val tabSelected =
                    RewardsViewModel.TabSelected.entries[binding.selectorGroup.selectedTabPosition]
                analytics.trackEventUserAction(
                    Analytics.ParamValue.IDENTIFY_PROBLEMS.paramValue,
                    Analytics.Screen.DEVICE_REWARDS.screenName,
                    Pair(FirebaseAnalytics.Param.ITEM_ID, tabSelected.analyticsValue)
                )
                if (tabSelected == RewardsViewModel.TabSelected.LATEST) {
                    navigator.showRewardDetails(requireContext(), model.device, it)
                } else {
                    navigator.showRewardsList(requireContext(), model.device)
                }
            }
            binding.rewardsMainCard.setVisible(true)
        }

        model.onTotalRewards().observe(viewLifecycleOwner) { rewards ->
            rewards?.let {
                binding.totalRewards.text =
                    getString(R.string.wxm_amount, formatTokens(rewards.toBigDecimal()))
            }
        }

        model.onLoading().observe(viewLifecycleOwner) {
            if (it && binding.swiperefresh.isRefreshing) {
                binding.progress.visibility = View.INVISIBLE
            } else if (it) {
                binding.rewardsMainCard.setVisible(false)
                binding.progress.visibility = View.VISIBLE
            } else {
                binding.swiperefresh.isRefreshing = false
                binding.progress.visibility = View.INVISIBLE
            }
        }

        model.onError().observe(viewLifecycleOwner) {
            binding.rewardsMainCard.setVisible(false)
            showSnackbarMessage(it.errorMessage, it.retryFunction)
        }

        binding.swiperefresh.setOnRefreshListener {
            model.fetchRewardsFromNetwork(
                RewardsViewModel.TabSelected.entries[binding.selectorGroup.selectedTabPosition]
            )
        }

        binding.selectorGroup.onTabSelected {
            trackRangeToggle(RewardsViewModel.TabSelected.entries[it.position].analyticsValue)
            model.fetchRewards(RewardsViewModel.TabSelected.entries[it.position])
        }

        binding.seeDetailedRewards.setOnClickListener {
            navigator.showRewardsList(requireContext(), model.device)
        }

        model.fetchRewardsFromNetwork()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.DEVICE_REWARDS, RewardsFragment::class.simpleName
        )
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

    private fun trackRangeToggle(newRange: String) {
        analytics.trackEventSelectContent(
            Analytics.ParamValue.REWARDS_CARD.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, model.device.id),
            Pair(Analytics.CustomParam.STATE.paramName, newRange)
        )
    }
}
