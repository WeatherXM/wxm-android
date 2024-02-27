package com.weatherxm.ui.rewardslist

import android.os.Bundle
import android.view.View
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Reward
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityRewardsListBinding
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.DailyReward
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.parcelable
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
        binding.toolbar.subtitle = device.getDefaultOrFriendlyName()

        // Initialize the adapter with empty data
        adapter = RewardsListAdapter(
            device.relation,
            onRewardDetails = {
                analytics.trackEventUserAction(
                    Analytics.ParamValue.IDENTIFY_PROBLEMS.paramValue,
                    Analytics.Screen.DEVICE_REWARD_TRANSACTIONS.screenName
                )
                navigator.showRewardDetails(
                    this,
                    device,
                    Reward(
                        it.rewardTimestamp,
                        it.actualReward,
                        0F,
                        it.actualReward,
                        it.rewardScore,
                        it.annotationSummary
                    )
                )
            },
            onEndOfData = { endOfDataListener() })
        binding.recycler.adapter = adapter

        model.onFirstPageRewards().observe(this) {
            updateUIFirstPage(it)
        }

        model.onNewRewardsPage().observe(this) {
            updateUINewPage(it)
        }

        model.fetchFirstPageRewards(deviceId)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Analytics.Screen.DEVICE_REWARD_TRANSACTIONS, this::class.simpleName)
    }

    private fun updateUIFirstPage(resource: Resource<List<DailyReward>>) {
        when (resource.status) {
            Status.SUCCESS -> {
                if (!resource.data.isNullOrEmpty()) {
                    adapter.submitList(resource.data)
                    binding.recycler.visibility = View.VISIBLE
                    binding.empty.visibility = View.GONE
                } else {
                    binding.empty.animation(R.raw.anim_empty_devices, false)
                    binding.empty.title(getString(R.string.no_transactions_title))
                    binding.empty.subtitle(getString(R.string.info_come_back_later))
                    binding.empty.listener(null)
                    binding.empty.visibility = View.VISIBLE
                    binding.recycler.visibility = View.GONE
                }
            }
            Status.ERROR -> {
                Timber.d("Got error: $resource.message")
                binding.recycler.visibility = View.GONE
                binding.empty.animation(R.raw.anim_error)
                binding.empty.title(getString(R.string.error_transactions_no_data))
                binding.empty.subtitle(resource.message)
                binding.empty.action(getString(R.string.action_retry))
                binding.empty.listener { model.fetchFirstPageRewards(deviceId) }
                binding.empty.visibility = View.VISIBLE
            }
            Status.LOADING -> {
                binding.recycler.visibility = View.GONE
                binding.empty.clear()
                binding.empty.animation(R.raw.anim_loading)
                binding.empty.visibility = View.VISIBLE
            }
        }
    }

    private fun updateUINewPage(resource: Resource<List<DailyReward>>) {
        when (resource.status) {
            Status.SUCCESS -> {
                if (!resource.data.isNullOrEmpty()) {
                    adapter.submitList(resource.data)
                    adapter.notifyDataSetChanged()
                }
                binding.loadingNewPage.visibility = View.GONE
            }
            Status.ERROR -> {
                Timber.d("Got error: $resource.message")
                binding.loadingNewPage.visibility = View.GONE
            }
            Status.LOADING -> {
                binding.loadingNewPage.visibility = View.VISIBLE
            }
        }
    }

    private fun endOfDataListener() {
        model.fetchNewPageRewards(deviceId)
    }
}

