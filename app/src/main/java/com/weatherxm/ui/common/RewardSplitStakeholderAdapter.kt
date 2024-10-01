package com.weatherxm.ui.common

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.models.RewardSplit
import com.weatherxm.databinding.ListItemRewardSplitStakeholderBinding
import com.weatherxm.util.Mask
import com.weatherxm.util.NumberUtils.formatTokens

class RewardSplitStakeholderAdapter(
    private val walletAddress: String,
    private val isInStationSettings: Boolean
) : ListAdapter<RewardSplit, RewardSplitStakeholderAdapter.RewardSplitStakeholderViewHolder>(
    RewardSplitStakeholderDiffCallback()
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RewardSplitStakeholderViewHolder {
        val binding = ListItemRewardSplitStakeholderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RewardSplitStakeholderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RewardSplitStakeholderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RewardSplitStakeholderViewHolder(
        private val binding: ListItemRewardSplitStakeholderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: RewardSplit) {
            if (item.wallet == walletAddress) {
                binding.address.setTypeface(null, Typeface.BOLD)
                binding.amount.setTypeface(null, Typeface.BOLD)
                binding.percentage.setTypeface(null, Typeface.BOLD)
                binding.address.text = itemView.context.getString(
                    R.string.reward_split_stakeholder_you,
                    Mask.maskHash(item.wallet)
                )
            } else {
                binding.address.text = Mask.maskHash(item.wallet)
            }

            if (isInStationSettings) {
                binding.percentage.text = "${item.stake}%"
            } else {
                binding.amount.text =
                    itemView.context.getString(R.string.wxm_amount, formatTokens(item.reward))
                binding.percentage.text = "(${item.stake}%)"
            }
        }
    }
}

class RewardSplitStakeholderDiffCallback : DiffUtil.ItemCallback<RewardSplit>() {

    override fun areItemsTheSame(
        oldItem: RewardSplit,
        newItem: RewardSplit
    ): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: RewardSplit,
        newItem: RewardSplit
    ): Boolean {
        return oldItem.reward == newItem.reward &&
            oldItem.stake == newItem.stake &&
            oldItem.wallet == newItem.wallet
    }
}
