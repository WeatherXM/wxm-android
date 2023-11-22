package com.weatherxm.ui.rewarddetails

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.databinding.ActivityRewardDetailsBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_REWARDS_OBJECT
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIRewardObject
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.util.Analytics
import com.weatherxm.util.Rewards.formatLostRewards
import com.weatherxm.util.applyInsets
import com.weatherxm.util.setHtml
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class RewardDetailsActivity : AppCompatActivity(), KoinComponent, RewardProblemsListener {
    private lateinit var binding: ActivityRewardDetailsBinding
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        val device = intent?.extras?.getParcelable<UIDevice>(ARG_DEVICE)
        val rewardsObject = intent?.extras?.getParcelable<UIRewardObject>(ARG_REWARDS_OBJECT)
        if (device == null || rewardsObject == null) {
            Timber.d("Could not start RewardDetailsActivity. Device or Rewards Object is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        with(binding.toolbar) {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
            subtitle = device.getDefaultOrFriendlyName()
            if (rewardsObject.txHash.isNullOrEmpty()) {
                menu.removeItem(R.id.view_transaction)
            }
            setOnMenuItemClickListener {
                onMenuItem(it, rewardsObject.txHash ?: "")
            }
        }

        binding.contactSupportBtn.setOnClickListener {
            navigator.sendSupportEmail(
                context = this,
                subject = getString(R.string.support_email_rewards_subject),
                source = Analytics.ParamValue.REWARD_ANNOTATIONS.paramValue
            )
        }
        binding.contactSupportBtn.setVisible(device.relation == DeviceRelation.OWNED)

        binding.rewardsContentCard.updateUI(
            rewardsObject,
            device.relation,
            onInfoButton = { title, htmlMessage ->
                navigator.showMessageDialog(
                    supportFragmentManager, title = title, htmlMessage = htmlMessage
                )
            }
        )
        updateErrors(device, rewardsObject)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.DEVICE_REWARD_DETAILS, RewardDetailsActivity::class.simpleName
        )
    }

    private fun updateErrors(device: UIDevice, data: UIRewardObject) {
        val hasAnnotations = data.annotations.isNotEmpty()
        binding.problemsFoundTitle.setVisible(hasAnnotations)
        binding.problemsFoundDesc.setVisible(hasAnnotations)
        binding.problemsList.setVisible(hasAnnotations)
        if (data.lostRewards == 0F && data.periodMaxReward == 0F) {
            binding.problemsFoundDesc.setHtml(getString(R.string.problems_found_desc_no_rewards))
        } else if (((data.lostRewards) ?: 0F) == 0F) {
            binding.problemsFoundDesc.setText(R.string.problems_found_desc_without_lost_rewards)
        } else {
            val lostRewards = formatLostRewards(data.lostRewards)
            binding.problemsFoundDesc.setHtml(getString(R.string.problems_found_desc, lostRewards))
        }

        val adapter = RewardProblemsAdapter(device, data.rewardScore, this)
        binding.problemsList.adapter = adapter

        adapter.submitList(data.annotations)
    }

    private fun onMenuItem(menuItem: MenuItem, txHash: String): Boolean {
        return when (menuItem.itemId) {
            R.id.view_transaction -> {
                analytics.trackEventSelectContent(
                    contentType = Analytics.ParamValue.REWARD_DETAILS_VIEW_TX.paramValue
                )
                navigator.openWebsite(this, "${getString(R.string.blockchain_explorer)}$txHash")
                true
            }
            R.id.read_more -> {
                analytics.trackEventSelectContent(
                    contentType = Analytics.ParamValue.REWARD_DETAILS_READ_MORE.paramValue
                )
                navigator.openWebsite(this, getString(R.string.docs_url_reward_mechanism))
                true
            }
            else -> false
        }
    }

    override fun onAddWallet() {
        navigator.showConnectWallet(this)
    }

    override fun onUpdateFirmware(device: UIDevice) {
        navigator.showDeviceHeliumOTA(this, device, false)
    }

    override fun onContactSupport(device: UIDevice, annotationTitle: String) {
        navigator.sendSupportEmail(
            context = this,
            subject = getString(R.string.support_email_rewards_subject),
            body = getString(R.string.support_email_rewards_body, device.label, annotationTitle),
            source = Analytics.ParamValue.REWARD_ANNOTATIONS.paramValue
        )
    }

    override fun onDocumentation(url: String) {
        navigator.openWebsite(this, url)
    }
}
