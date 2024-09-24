package com.weatherxm.ui.devicesrewards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.repository.RewardsRepositoryImpl.Companion.RewardsSummaryMode
import com.weatherxm.databinding.ListItemDeviceRewardsBinding
import com.weatherxm.ui.common.DeviceTotalRewards
import com.weatherxm.ui.common.DeviceTotalRewardsDetails
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.hide
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.show
import com.weatherxm.ui.common.visible
import com.weatherxm.util.Rewards.formatTokens
import com.weatherxm.util.initRewardsBreakdownChart

class DeviceRewardsAdapter(
    private val onFetchNewData: (String, Int, Int) -> Unit,
    private val onCancelFetching: (Int) -> Unit,
) : ListAdapter<DeviceTotalRewards, DeviceRewardsAdapter.DeviceRewardsViewHolder>(
    DeviceRewardsDiffCallback()
) {
    private val expandedPositions = mutableSetOf(0)

    fun replaceItem(position: Int, details: DeviceTotalRewardsDetails) {
        getItem(position).details = details
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceRewardsViewHolder {
        val binding = ListItemDeviceRewardsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceRewardsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceRewardsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceRewardsViewHolder(
        private val binding: ListItemDeviceRewardsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        val adapter = DeviceRewardsBoostAdapter()
        private var ignoreRangeChipListener = false

        fun bind(item: DeviceTotalRewards) {
            binding.root.setOnClickListener {
                onExpandClick(item)
            }

            binding.chartRangeSelector.listener {
                if (!ignoreRangeChipListener) {
                    onFetchNewData.invoke(item.id, absoluteAdapterPosition, it)
                }
            }

            binding.name.text = item.name
            binding.amount.text =
                itemView.context.getString(R.string.wxm_amount, formatTokens(item.total))

            if (expandedPositions.contains(absoluteAdapterPosition)) {
                binding.openDeviceRewards.setImageResource(R.drawable.ic_arrow_up)
                binding.detailsWithLoadingContainer.show()
            }

            preCheckMode(item.details.mode)
            when (item.details.status) {
                Status.SUCCESS -> {
                    onDetails(item.details)
                }
                Status.ERROR -> {
                    onError(item.id)
                }
                Status.LOADING -> {
                    binding.chartRangeSelector.disable()
                    binding.detailsContainer.invisible()
                    binding.earnedBy.invisible()
                    binding.retryCard.visible(false)
                    binding.detailsStatus.visible(true)
                }
            }
        }

        private fun preCheckMode(mode: RewardsSummaryMode?) {
            ignoreRangeChipListener = true
            when (mode) {
                RewardsSummaryMode.WEEK -> binding.chartRangeSelector.checkWeek()
                RewardsSummaryMode.MONTH -> binding.chartRangeSelector.checkMonth()
                RewardsSummaryMode.YEAR -> binding.chartRangeSelector.checkYear()
                else -> throw NotImplementedError("Unknown rewards mode $mode")
            }.also {
                ignoreRangeChipListener = false
            }
        }

        private fun onDetails(details: DeviceTotalRewardsDetails) {
            binding.earnedBy.text = formatTokens(details.total)
            binding.boostsRecycler.adapter = adapter
            adapter.submitList(details.boosts)

            binding.rewardBreakdownChart.initRewardsBreakdownChart(
                details.baseChartData,
                details.betaChartData,
                details.otherChartData,
                details.totals,
                details.datesChartTooltip
            )

            binding.baseRewardsLegend.visible(details.baseChartData.isDataValid())
            binding.betaRewardsLegend.visible(details.betaChartData.isDataValid())
            binding.othersRewardsLegend.visible(details.otherChartData.isDataValid())
            binding.retryCard.visible(false)
            binding.detailsStatus.visible(false)
            binding.chartRangeSelector.enable()
            binding.earnedBy.visible(true)
            binding.detailsContainer.visible(true)
        }

        private fun onExpandClick(item: DeviceTotalRewards) {
            /**
             * We define a new variable here because toggleVisibility uses an animation and has a
             * delay so we cannot use `binding.hourlyRecycler.isVisible` directly`.
             */
            val willBeExpanded = !binding.detailsWithLoadingContainer.isVisible

            binding.openDeviceRewards.setImageResource(
                if (willBeExpanded) {
                    R.drawable.ic_arrow_up
                } else {
                    R.drawable.ic_arrow_down
                }
            )

            if (willBeExpanded) {
                expandedPositions.add(absoluteAdapterPosition)
                if (item.details.status != Status.SUCCESS) {
                    onFetchNewData.invoke(
                        item.id,
                        absoluteAdapterPosition,
                        binding.chartRangeSelector.checkedChipId()
                    )
                } else {
                    binding.detailsWithLoadingContainer.show()
                }
            } else {
                onCancelFetching.invoke(absoluteAdapterPosition)
                expandedPositions.remove(absoluteAdapterPosition)
                binding.detailsWithLoadingContainer.hide()
            }
        }

        private fun onError(deviceId: String) {
            binding.detailsStatus.visible(false)
            binding.detailsContainer.invisible()
            binding.earnedBy.invisible()
            binding.retryCard.listener {
                onFetchNewData.invoke(
                    deviceId,
                    absoluteAdapterPosition,
                    binding.chartRangeSelector.checkedChipId()
                )
            }
            binding.chartRangeSelector.enable()
            binding.retryCard.visible(true)
        }
    }
}

class DeviceRewardsDiffCallback : DiffUtil.ItemCallback<DeviceTotalRewards>() {

    override fun areItemsTheSame(
        oldItem: DeviceTotalRewards,
        newItem: DeviceTotalRewards
    ): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: DeviceTotalRewards,
        newItem: DeviceTotalRewards
    ): Boolean {
        return oldItem.id == newItem.id &&
            oldItem.name == newItem.name &&
            oldItem.total == newItem.total &&
            oldItem.details == newItem.details &&
            oldItem.details.total == newItem.details.total &&
            oldItem.details.mode == newItem.details.mode &&
            oldItem.details.status == newItem.details.status &&
            oldItem.details.totals.size == newItem.details.totals.size &&
            oldItem.details.boosts.size == newItem.details.boosts.size &&
            oldItem.details.datesChartTooltip.size == newItem.details.datesChartTooltip.size &&
            oldItem.details.baseChartData == newItem.details.baseChartData &&
            oldItem.details.betaChartData == newItem.details.betaChartData &&
            oldItem.details.otherChartData == newItem.details.otherChartData
    }
}
