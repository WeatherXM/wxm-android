package com.weatherxm.ui.rewardissues

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.RewardDetails
import com.weatherxm.databinding.ActivityRewardIssuesBinding
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_REWARD_DETAILS
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.common.getClassSimpleName
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import timber.log.Timber

class RewardIssuesActivity : BaseActivity(), RewardIssuesListener {
    private lateinit var binding: ActivityRewardIssuesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardIssuesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        val reward = intent?.extras?.parcelable<RewardDetails>(ARG_REWARD_DETAILS)
        val device = intent?.extras?.parcelable<UIDevice>(ARG_DEVICE)
        if (reward == null || device == null) {
            Timber.d("Could not start RewardIssuesActivity. Reward or Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        with(binding.toolbar) {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
        val subtitle =
            "${getString(R.string.report_for, reward.timestamp.getFormattedDate(true))} (UTC)"
        binding.header.subtitle(subtitle)

        val adapter = RewardIssuesAdapter(device, this)
        binding.recycler.adapter = adapter

        adapter.submitList(reward.toSortedAnnotations())
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.REWARD_ISSUES, getClassSimpleName())
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
            actionName = AnalyticsService.ParamValue.REWARD_ISSUES_ERROR.paramValue,
            contentType = null,
            Pair(FirebaseAnalytics.Param.ITEM_ID, group ?: String.empty())
        )
    }
}
