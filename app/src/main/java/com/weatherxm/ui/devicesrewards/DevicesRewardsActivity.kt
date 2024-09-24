package com.weatherxm.ui.devicesrewards

import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.ui.common.Status
import com.weatherxm.databinding.ActivityDevicesRewardsBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.Rewards.formatTokens
import com.weatherxm.util.initTotalEarnedChart
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class DevicesRewardsActivity : BaseActivity() {
    private val model: DevicesRewardsViewModel by viewModel {
        parametersOf(intent.parcelable<DevicesRewards>(Contracts.ARG_DEVICES_REWARDS))
    }
    private lateinit var binding: ActivityDevicesRewardsBinding

    lateinit var adapter: DeviceRewardsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDevicesRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.buyStationBtn.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.shop_url))
        }

        binding.totalEarnedRangeSelector.listener {
            model.getDevicesRewardsByRangeTotals(it)
        }

        model.onRewardsByRange().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    binding.totalEarnedStatus.visible(false)
                    binding.retryCard.visible(false)
                    binding.totalEarnedRangeSelector.enable()
                    binding.totalEarned.text =
                        getString(R.string.wxm_amount, formatTokens(it.data?.total))
                    it.data?.let { data ->
                        binding.totalEarnedChart.initTotalEarnedChart(
                            data.lineChartData,
                            data.datesChartTooltip
                        )
                    }
                    binding.totalEarned.visible(true)
                    binding.totalEarnedChart.visible(true)
                }
                Status.ERROR -> {
                    onError()
                }
                Status.LOADING -> {
                    binding.totalEarnedRangeSelector.disable()
                    binding.totalEarnedChart.invisible()
                    binding.totalEarned.invisible()
                    binding.retryCard.visible(false)
                    binding.totalEarnedStatus.clear().animation(R.raw.anim_loading).visible(true)
                }
            }
        }

        model.onDeviceRewardDetails().observe(this) {
            adapter.replaceItem(it.first, it.second)
        }

        adapter = DeviceRewardsAdapter(
            onFetchNewData = { deviceId, position, checkedRangeChipId ->
                model.getDeviceRewardsByRange(deviceId, position, checkedRangeChipId)
            },
            onCancelFetching = { position ->
                model.cancelFetching(position)
            }
        )
        binding.devicesRecycler.adapter = adapter

        val hasDevices = model.rewards.devices.isNotEmpty()
        if (hasDevices) {
            binding.totalEarnedStationsTitle.text = resources.getQuantityString(
                R.plurals.total_earned_stations,
                model.rewards.devices.size,
                model.rewards.devices.size
            )
            binding.totalEarnedStations.text =
                getString(R.string.wxm_amount, formatTokens(model.rewards.total))
            binding.lastRun.text = getString(R.string.reward, formatTokens(model.rewards.latest))
            binding.mainContainer.visible(true)
            binding.emptyRewardsCard.visible(model.rewards.total == 0F)

            adapter.submitList(model.rewards.devices)

            binding.totalEarnedRangeSelector.checkWeek()
            model.getDevicesRewardsByRangeTotals()
            model.getDeviceRewardsByRange(model.rewards.devices[0].id, 0)
        } else {
            binding.noStationsContainer.visible(true)
        }
    }

    private fun onError() {
        binding.totalEarnedRangeSelector.enable()
        binding.totalEarnedChart.invisible()
        binding.totalEarned.invisible()
        binding.totalEarnedStatus.visible(false)
        binding.retryCard.listener {
            model.getDevicesRewardsByRangeTotals(
                binding.totalEarnedRangeSelector.checkedChipId()
            )
        }
        binding.retryCard.visible(true)
    }
}
