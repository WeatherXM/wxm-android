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
import com.weatherxm.ui.common.hide
import com.weatherxm.ui.common.show
import com.weatherxm.ui.common.visible
import com.weatherxm.util.Rewards.formatTokens
import com.weatherxm.util.initRewardsBreakdownChart

class DeviceRewardsAdapter(
    private val onExpandToggle: (Int, Boolean, String) -> Unit,
    private val onRangeChipClicked: (Int, Int, String) -> Unit,
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
                    invokeOnRangeChip(it, item.id)
                }
            }

            if (expandedPositions.contains(absoluteAdapterPosition)) {
                if (absoluteAdapterPosition == 0 && item.details == null) {
                    onExpandToggle.invoke(absoluteAdapterPosition, true, item.id)
                }
                binding.openDeviceRewards.setImageResource(R.drawable.ic_arrow_up)
                binding.detailsWithLoadingContainer.show()
            }

            binding.name.text = item.name
            binding.amount.text =
                itemView.context.getString(R.string.wxm_amount, formatTokens(item.total))

            // If details is null we are in the LOADING state
            item.details?.let {
                if (it.fetchError) {
                    onError(item.id)
                } else {
                    onDetails(it)
                }
            } ?: kotlin.run {
                binding.retryCard.visible(false)
                binding.detailsStatus.visible(true)
            }
        }

        private fun onDetails(details: DeviceTotalRewardsDetails) {
            binding.earnedBy.text = formatTokens(details.total)
            binding.boostsRecycler.adapter = adapter
            adapter.submitList(details.boosts)

            ignoreRangeChipListener = true
            when (details.mode) {
                RewardsSummaryMode.WEEK -> binding.chartRangeSelector.checkWeek()
                RewardsSummaryMode.MONTH -> binding.chartRangeSelector.checkMonth()
                RewardsSummaryMode.YEAR -> binding.chartRangeSelector.checkYear()
                else -> binding.chartRangeSelector.clearCheck()
            }.also {
                ignoreRangeChipListener = false
            }
            binding.rewardBreakdownChart.initRewardsBreakdownChart(
                details.baseChartData,
                details.betaChartData,
                details.otherChartData,
                details.totalsForTooltip,
                details.datesChartTooltip
            )

            binding.baseRewardsLegend.visible(details.baseChartData.isDataValid())
            binding.betaRewardsLegend.visible(details.betaChartData.isDataValid())
            binding.othersRewardsLegend.visible(details.otherChartData.isDataValid())
            binding.retryCard.visible(false)
            binding.detailsStatus.visible(false)
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
                binding.detailsWithLoadingContainer.show()
                expandedPositions.add(absoluteAdapterPosition)
            } else {
                binding.detailsWithLoadingContainer.hide()
                expandedPositions.remove(absoluteAdapterPosition)
            }

            onExpandToggle.invoke(absoluteAdapterPosition, willBeExpanded, item.id)
        }

        private fun onError(deviceId: String) {
            binding.detailsStatus.visible(false)
            binding.retryCard.listener {
                invokeOnRangeChip(binding.chartRangeSelector.checkedChipId(), deviceId)
            }
            binding.retryCard.visible(true)
        }

        private fun invokeOnRangeChip(checkedChipId: Int, deviceId: String) {
            binding.detailsStatus.animation(R.raw.anim_loading).visible(true)
            binding.detailsContainer.visible(false)
            binding.retryCard.visible(false)
            onRangeChipClicked.invoke(absoluteAdapterPosition, checkedChipId, deviceId)
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
            oldItem.details == newItem.details
    }
}
