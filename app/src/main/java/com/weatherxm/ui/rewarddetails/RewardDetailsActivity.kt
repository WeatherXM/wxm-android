package com.weatherxm.ui.rewarddetails

import android.os.Bundle
import android.view.MenuItem
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.data.Reward
import com.weatherxm.databinding.ActivityRewardDetailsBinding
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_REWARD
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.Analytics
import timber.log.Timber

class RewardDetailsActivity : BaseActivity(), RewardProblemsListener {
    private lateinit var binding: ActivityRewardDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        val device = intent?.extras?.parcelable<UIDevice>(ARG_DEVICE)
        val reward = intent?.extras?.parcelable<Reward>(ARG_REWARD)
        if (device == null || reward == null) {
            Timber.d("Could not start RewardDetailsActivity. Device or Rewards Object is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        with(binding.toolbar) {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
            subtitle = device.getDefaultOrFriendlyName()
            setOnMenuItemClickListener {
                onMenuItem(it)
            }
        }

        binding.contactSupportBtn.setOnClickListener {
            navigator.openSupportCenter(this, Analytics.ParamValue.REWARD_ANNOTATIONS.paramValue)
        }
        binding.contactSupportBtn.setVisible(device.isOwned())

        binding.rewardsContentCard.updateUI(reward, isInRewardDetails = true)
        updateErrors(device, reward)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Analytics.Screen.DEVICE_REWARD_DETAILS, this::class.simpleName)
    }

    private fun updateErrors(device: UIDevice, data: Reward) {
        binding.problemsFoundTitle.setVisible(data.annotationSummary?.isNotEmpty() == true)
        binding.problemsList.setVisible(data.annotationSummary?.isNotEmpty() == true)

        val adapter = RewardProblemsAdapter(device, this)
        binding.problemsList.adapter = adapter

        adapter.submitList(data.annotationSummary)
    }

    private fun onMenuItem(menuItem: MenuItem): Boolean {
        return if (menuItem.itemId == R.id.read_more) {
            analytics.trackEventSelectContent(
                contentType = Analytics.ParamValue.REWARD_DETAILS_READ_MORE.paramValue
            )
            navigator.openWebsite(this, getString(R.string.docs_url_reward_mechanism))
            true
        } else {
            false
        }
    }

    override fun onAddWallet(group: String?) {
        trackUserActionOnErrors(group)
        navigator.showConnectWallet(this)
    }

    override fun onDocumentation(url: String, group: String?) {
        trackUserActionOnErrors(group)
        navigator.openWebsite(this, url)
    }

    override fun onEditLocation(device: UIDevice, group: String?) {
        trackUserActionOnErrors(group)
        navigator.showEditLocation(null, this, device)
    }

    private fun trackUserActionOnErrors(group: String?) {
        analytics.trackEventUserAction(
            actionName = Analytics.ParamValue.REWARD_DETAILS_ERROR.paramValue,
            contentType = null,
            Pair(FirebaseAnalytics.Param.ITEM_ID, group ?: String.empty())
        )
    }
}
