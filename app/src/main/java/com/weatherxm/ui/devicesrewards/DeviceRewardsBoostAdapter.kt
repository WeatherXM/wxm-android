package com.weatherxm.ui.devicesrewards

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.models.BoostCode
import com.weatherxm.databinding.ListItemDeviceRewardsBoostBinding
import com.weatherxm.ui.common.DeviceTotalRewardsBoost
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.compose.RoundedRangeView
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.NumberUtils.formatTokens
import timber.log.Timber

class DeviceRewardsBoostAdapter :
    ListAdapter<DeviceTotalRewardsBoost, DeviceRewardsBoostAdapter.DeviceRewardsBoostViewHolder>(
        DeviceRewardsBoostDiffCallback()
    ) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DeviceRewardsBoostViewHolder {
        val binding = ListItemDeviceRewardsBoostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceRewardsBoostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceRewardsBoostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DeviceRewardsBoostViewHolder(
        private val binding: ListItemDeviceRewardsBoostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @Suppress("MagicNumber")
        @SuppressLint("SetTextI18n")
        fun bind(item: DeviceTotalRewardsBoost) {
            val boostCode = item.boostCode
            var progressSliderActiveColor: Int = R.color.other_reward
            var progressSliderInactiveColor: Int = R.color.other_reward_fill
            try {
                if (boostCode == null) {
                    binding.title.text =
                        itemView.context.getString(R.string.other_boost_reward_details)
                } else {
                    val isBetaRewards = boostCode == BoostCode.beta_rewards.name
                    val isCorrectionRewards = boostCode.startsWith(BoostCode.correction.name, true)
                    val isCellBountyReward =
                        boostCode.startsWith(BoostCode.cell_bounty.name, true)
                    val isRolloutRewards = boostCode == BoostCode.trov2.name

                    if (isBetaRewards) {
                        binding.title.text =
                            itemView.context.getString(R.string.beta_reward_details)
                        progressSliderActiveColor = R.color.beta_rewards_fill
                        progressSliderInactiveColor = R.color.beta_rewards_color
                    } else if (isCorrectionRewards) {
                        binding.title.text =
                            itemView.context.getString(R.string.compensation_reward_details)
                        progressSliderActiveColor = R.color.correction_rewards_color
                        progressSliderInactiveColor = R.color.correction_rewards_fill
                    } else if (isCellBountyReward) {
                        binding.title.text =
                            itemView.context.getString(R.string.cell_bounty_reward_details)
                        progressSliderActiveColor = R.color.cell_bounty_reward
                        progressSliderInactiveColor = R.color.cell_bounty_reward_fill
                    } else if (isRolloutRewards) {
                        binding.title.text =
                            itemView.context.getString(R.string.rollouts_reward_details)
                    } else {
                        binding.title.text =
                            itemView.context.getString(R.string.other_boost_reward_details)
                    }
                }
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Unsupported Boost Code: $boostCode")
                binding.title.text = itemView.context.getString(R.string.other_boost_reward_details)
            }

            item.completedPercentage?.let {
                binding.boostProgress.text = "$it%"
                binding.boostProgressSlider.setContent {
                    RoundedRangeView(
                        23.dp,
                        0F..it.toFloat(),
                        0F..100F,
                        progressSliderInactiveColor,
                        progressSliderActiveColor
                    )
                }
            } ?: binding.boostProgressSlider.visible(false)

            if (item.currentRewards == null) {
                binding.totalTokensSoFar.visible(false)
                binding.totalTokensSoFarTitle.visible(false)
                binding.firstDivider.visible(false)
            } else {
                binding.totalTokensSoFar.text = itemView.context.getString(
                    R.string.wxm_amount,
                    formatTokens(item.currentRewards)
                )
            }
            if (item.maxRewards == null) {
                binding.totalTokensMax.visible(false)
                binding.totalTokensMaxTitle.visible(false)
                binding.secondDivider.visible(false)
            } else {
                binding.totalTokensMax.text = itemView.context.getString(
                    R.string.wxm_amount,
                    formatTokens(item.maxRewards)
                )
            }

            val boostStartDate = item.boostPeriodStart.getFormattedDate(true, includeComma = false)
            val boostStopDate = item.boostPeriodEnd.getFormattedDate(true, includeComma = false)
            binding.boostPeriod.text = "$boostStartDate - $boostStopDate"
        }
    }
}

class DeviceRewardsBoostDiffCallback : DiffUtil.ItemCallback<DeviceTotalRewardsBoost>() {

    override fun areItemsTheSame(
        oldItem: DeviceTotalRewardsBoost,
        newItem: DeviceTotalRewardsBoost
    ): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: DeviceTotalRewardsBoost,
        newItem: DeviceTotalRewardsBoost
    ): Boolean {
        return oldItem.boostCode == newItem.boostCode &&
            oldItem.maxRewards == newItem.maxRewards &&
            oldItem.currentRewards == newItem.currentRewards &&
            oldItem.boostPeriodEnd == newItem.boostPeriodEnd &&
            oldItem.boostPeriodStart == newItem.boostPeriodStart &&
            oldItem.completedPercentage == newItem.completedPercentage
    }
}
