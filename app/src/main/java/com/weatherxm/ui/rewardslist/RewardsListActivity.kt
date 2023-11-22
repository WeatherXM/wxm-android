package com.weatherxm.ui.rewardslist

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityRewardsListBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIRewardObject
import com.weatherxm.ui.common.toast
import com.weatherxm.util.Analytics
import com.weatherxm.util.applyInsets
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber

class RewardsListActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityRewardsListBinding
    private val model: RewardsListViewModel by viewModels()
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()

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

        val device = intent?.extras?.getParcelable<UIDevice>(ARG_DEVICE)
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
                    Analytics.Screen.DEVICE_REWARD_TRANSACTIONS.screenName,
                    Pair(FirebaseAnalytics.Param.ITEM_ID, it.txHash ?: "")
                )
                navigator.showRewardDetails(this, device, it)
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
        analytics.trackScreen(
            Analytics.Screen.DEVICE_REWARD_TRANSACTIONS,
            RewardsListActivity::class.simpleName
        )
    }

    private fun updateUIFirstPage(resource: Resource<List<UIRewardObject>>) {
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

    private fun updateUINewPage(resource: Resource<List<UIRewardObject>>) {
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

