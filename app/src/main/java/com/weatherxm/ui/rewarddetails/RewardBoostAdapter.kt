package com.weatherxm.ui.rewarddetails

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.BoostReward
import com.weatherxm.databinding.ListItemRewardBoostBinding
import com.weatherxm.util.Rewards.formatTokens

class RewardBoostAdapter(
    private val listener: RewardBoostListener
) : ListAdapter<BoostReward, RewardBoostAdapter.RewardBoostViewHolder>(
    RewardBoostDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardBoostViewHolder {
        val binding = ListItemRewardBoostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RewardBoostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RewardBoostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RewardBoostViewHolder(private val binding: ListItemRewardBoostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BoostReward) {
            binding.root.setOnClickListener {
                listener.onBoostReward(item)
            }
            binding.title.text = item.title
            binding.desc.text = item.description
            binding.amount.text =
                itemView.context.getString(R.string.reward, formatTokens(item.actualReward))

            /**
             * Fallback in case img url is missing so the texts can be visible
             */
            if (item.imgUrl.isNullOrEmpty()) {
                binding.root.setCardBackgroundColor(
                    itemView.context.getColor(R.color.dark_background)
                )
            } else {
                // TODO: Load image from URL
            }
        }
    }
}

class RewardBoostDiffCallback : DiffUtil.ItemCallback<BoostReward>() {

    override fun areItemsTheSame(
        oldItem: BoostReward,
        newItem: BoostReward
    ): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: BoostReward,
        newItem: BoostReward
    ): Boolean {
        return oldItem.title == newItem.title && oldItem.docUrl == newItem.docUrl &&
            oldItem.imgUrl == newItem.imgUrl && oldItem.actualReward == newItem.actualReward &&
            oldItem.description == newItem.description && oldItem.maxReward == newItem.maxReward &&
            oldItem.rewardScore == newItem.rewardScore
    }
}

interface RewardBoostListener {
    fun onBoostReward(boost: BoostReward)
}
