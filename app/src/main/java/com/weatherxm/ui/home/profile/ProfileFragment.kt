package com.weatherxm.ui.home.profile

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.data.User
import com.weatherxm.databinding.FragmentProfileBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts.ARG_TOKEN_CLAIMED_AMOUNT
import com.weatherxm.ui.common.UIWalletRewards
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.home.HomeViewModel
import com.weatherxm.util.Analytics
import com.weatherxm.util.Mask
import com.weatherxm.util.Rewards.formatTokens
import com.weatherxm.util.Rewards.weiToETH
import com.weatherxm.util.applyInsets
import com.weatherxm.util.setCardStroke
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.math.BigInteger

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
                model.fetchUser()
            }
        }

    // Register the launcher for the rewards claim activity and wait for a possible result
    private val rewardsClaimLauncher =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                model.onClaimedResult(
                    result.data?.getSerializableExtra(ARG_TOKEN_CLAIMED_AMOUNT) as BigInteger
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        binding.root.applyInsets()

        binding.swiperefresh.setOnRefreshListener {
            model.fetchUser()
        }

        binding.walletContainerCard.setOnClickListener {
            navigator.showConnectWallet(connectWalletLauncher, this)
        }

        binding.settingsContainerCard.setOnClickListener {
            navigator.showPreferences(this)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.totalEarnedInfoBtn.setOnClickListener {
            navigator.showMessageDialog(
                childFragmentManager,
                title = getString(R.string.total_earned),
                message = getString(R.string.total_earned_desc)
            )
        }

        binding.totalClaimedInfoBtn.setOnClickListener {
            navigator.showMessageDialog(
                childFragmentManager,
                title = getString(R.string.total_claimed),
                message = getString(R.string.total_claimed_desc)
            )
        }

        model.onLoading().observe(viewLifecycleOwner) {
            if (it && !binding.swiperefresh.isRefreshing) {
                binding.progress.visibility = View.VISIBLE
            } else {
                binding.swiperefresh.isRefreshing = false
                binding.progress.visibility = View.INVISIBLE
            }
        }

        model.onUser().observe(viewLifecycleOwner) { resource ->
            Timber.d("Data updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    updateUserUI(resource.data)
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    resource.message?.let { context.toast(it) }
                }
                Status.LOADING -> {
                    // Do nothing
                }
            }
        }

        model.onWalletRewards().observe(viewLifecycleOwner) { resource ->
            Timber.d("Data updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    resource.data?.let {
                        updateRewardsUI(it)
                    }
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    resource.message?.let { context.toast(it) }
                }
                Status.LOADING -> {
                    // Do nothing
                }
            }
        }

        val tokenClaimingEnabled = model.isTokenClaimingEnabled()
        binding.totalsRewardsContainer.setVisible(tokenClaimingEnabled)
        binding.rewardsContainerCard.setVisible(tokenClaimingEnabled)

        model.fetchUser()
    }

    private fun updateRewardsUI(data: UIWalletRewards) {
        binding.rewards.clear()
        binding.totalEarnedValue.text =
            getString(R.string.wxm_amount, formatTokens(weiToETH(data.totalEarned.toBigDecimal())))
        binding.totalClaimedValue.text =
            getString(R.string.wxm_amount, formatTokens(weiToETH(data.totalClaimed.toBigDecimal())))
        if (data.allocated == BigInteger.ZERO) {
            binding.rewards.subtitle(getString(R.string.no_allocated_rewards))

            if (parentModel.hasDevices() == false) {
                binding.rewardsContainerCard.setCardStroke(R.color.colorPrimary, 2)
                binding.buyStationCard.actionPrimaryBtn(
                    getString(R.string.action_buy_station),
                    AppCompatResources.getDrawable(requireContext(), R.drawable.ic_cart),
                ) {
                    navigator.openWebsite(requireContext(), getString(R.string.shop_url))
                }.setVisible(true)
            }
        } else {
            binding.rewardsContainerCard.strokeWidth = 0
            binding.buyStationCard.setVisible(false)
            binding.rewards
                .subtitle(
                    getString(
                        R.string.wxm_amount, formatTokens(weiToETH(data.allocated.toBigDecimal()))
                    )
                )
                .action(getString(R.string.action_claim)) {
                    navigator.showRewardsClaiming(rewardsClaimLauncher, requireContext(), data)
                }
        }
    }

    private fun updateUserUI(user: User?) {
        binding.wallet.clear()
        binding.toolbar.subtitle = user?.email
        user?.wallet?.address?.let {
            binding.noWalletCard.setVisible(false)
            binding.walletContainerCard.strokeWidth = 0
            binding.wallet.chipSubtitle(Mask.maskHash(it))
        } ?: kotlin.run {
            binding.wallet.subtitle(getString(R.string.no_wallet_added))
            if (parentModel.hasDevices() == true) {
                binding.noWalletCard.setVisible(true)
                binding.walletContainerCard.setCardStroke(R.color.error, 2)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.PROFILE,
            ProfileFragment::class.simpleName
        )
    }
}
