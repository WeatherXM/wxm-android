package com.weatherxm.ui.devicesrewards

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.models.BoostCode
import com.weatherxm.databinding.ListItemDeviceRewardsBoostBinding
import com.weatherxm.ui.common.DeviceTotalRewardsBoost
import com.weatherxm.ui.common.visible
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

    inner class DeviceRewardsBoostViewHolder(
        private val binding: ListItemDeviceRewardsBoostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: DeviceTotalRewardsBoost) {
            val boostCode = item.boostCode
            try {
                if (boostCode == null) {
                    onUnknownBoost()
                } else {
                    val isBetaRewards = boostCode == BoostCode.beta_rewards.name
                    val isCorrectionRewards = boostCode.startsWith(BoostCode.correction.name, true)

                    if (isBetaRewards) {
                        binding.title.text =
                            itemView.context.getString(R.string.beta_reward_details)
                        binding.boostProgressSlider.trackActiveTintList =
                            itemView.context.getColorStateList(R.color.beta_rewards_fill)
                        binding.boostProgressSlider.trackInactiveTintList =
                            itemView.context.getColorStateList(R.color.beta_rewards_color)
                    } else if (isCorrectionRewards) {
                        binding.title.text =
                            itemView.context.getString(R.string.compensation_reward_details)
                        binding.boostProgressSlider.trackActiveTintList =
                            itemView.context.getColorStateList(R.color.correction_rewards_color)
                        binding.boostProgressSlider.trackInactiveTintList =
                            itemView.context.getColorStateList(R.color.correction_rewards_fill)
                    } else {
                        onUnknownBoost()
                    }
                }
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Unsupported Boost Code: $boostCode")
                onUnknownBoost()
            }

            item.completedPercentage?.let {
                binding.boostProgress.text = "$it%"
                binding.boostProgressSlider.values = listOf(it.toFloat())
            } ?: binding.boostProgressSlider.visible(false)

            binding.totalTokensSoFar.text =
                itemView.context.getString(R.string.wxm_amount, formatTokens(item.currentRewards))
            binding.totalTokensMax.text =
                itemView.context.getString(R.string.wxm_amount, formatTokens(item.maxRewards))

            val boostStartDate = item.boostPeriodStart.getFormattedDate(true, includeComma = false)
            val boostStopDate = item.boostPeriodEnd.getFormattedDate(true, includeComma = false)
            binding.boostPeriod.text = "$boostStartDate - $boostStopDate"
        }

        private fun onUnknownBoost() {
            binding.title.text = itemView.context.getString(R.string.other_boost_reward_details)
            binding.boostProgressSlider.trackActiveTintList =
                itemView.context.getColorStateList(R.color.other_reward)
            binding.boostProgressSlider.trackInactiveTintList =
                itemView.context.getColorStateList(R.color.other_reward_fill)
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
