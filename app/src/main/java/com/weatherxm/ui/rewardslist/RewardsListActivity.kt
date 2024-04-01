package com.weatherxm.ui.rewardslist

import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Reward
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityRewardsListBinding
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.TimelineReward
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.Analytics
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class RewardsListActivity : BaseActivity() {
    private lateinit var binding: ActivityRewardsListBinding
    private val model: RewardsListViewModel by viewModel()

    private lateinit var adapter: RewardsListAdapter
    private lateinit var deviceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val device = intent?.extras?.parcelable<UIDevice>(ARG_DEVICE)
        if (device == null) {
            Timber.d("Could not start TokenActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        deviceId = device.id

        // Initialize the adapter with empty data
        adapter = RewardsListAdapter(
            onRewardDetails = {
                analytics.trackEventUserAction(
                    Analytics.ParamValue.IDENTIFY_PROBLEMS.paramValue,
                    Analytics.Screen.DEVICE_REWARD_TRANSACTIONS.screenName
                )
                navigator.showRewardDetails(this, device, it)
            },
            onEndOfData = { endOfDataListener() }
        )
        binding.recycler.adapter = adapter

        model.onFirstPageRewards().observe(this) {
            updateUIFirstPage(it)
        }

        model.onNewRewardsPage().observe(this) {
            updateUINewPage(it)
        }

        model.onEndOfData().observe(this) {
            adapter.setData(it)
        }

        model.fetchFirstPageRewards(deviceId)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Analytics.Screen.DEVICE_REWARD_TRANSACTIONS, this::class.simpleName)
    }

    private fun updateUIFirstPage(resource: Resource<List<TimelineReward>>) {
        when (resource.status) {
            Status.SUCCESS -> {
                if (!resource.data.isNullOrEmpty()) {
                    adapter.setData(resource.data)
                    binding.recycler.setVisible(true)
                    binding.status.setVisible(false)
                    binding.emptyRewardsCard.setVisible(false)
                } else {
                    binding.recycler.setVisible(false)
                    binding.emptyRewardsCard.setVisible(true)
                }
            }
            Status.ERROR -> {
                Timber.d("Got error: $resource.message")
                binding.recycler.setVisible(false)
                binding.status.animation(R.raw.anim_error)
                    .title(getString(R.string.error_rewards_no_data))
                    .subtitle(resource.message)
                    .action(getString(R.string.action_retry))
                    .listener { model.fetchFirstPageRewards(deviceId) }
                    .setVisible(true)
            }
            Status.LOADING -> {
                binding.recycler.setVisible(false)
                binding.status.clear()
                    .animation(R.raw.anim_loading)
                    .setVisible(true)
            }
        }
    }

    private fun updateUINewPage(resource: Resource<List<TimelineReward>>) {
        when (resource.status) {
            Status.SUCCESS -> {
                if (!resource.data.isNullOrEmpty()) {
                    adapter.setData(resource.data)
                }
                binding.loadingNewPage.setVisible(false)
            }
            Status.ERROR -> {
                Timber.d("Got error: $resource.message")
                binding.loadingNewPage.setVisible(false)
            }
            Status.LOADING -> {
                binding.loadingNewPage.setVisible(true)
            }
        }
    }

    private fun endOfDataListener() {
        model.fetchNewPageRewards(deviceId)
    }
}

