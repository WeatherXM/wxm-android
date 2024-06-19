package com.weatherxm.ui.rewardboosts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.databinding.ListItemBoostDetailBinding
import com.weatherxm.ui.common.BoostDetailInfo
import com.weatherxm.ui.common.visible

class RewardBoostDetailAdapter :
    ListAdapter<BoostDetailInfo, RewardBoostDetailAdapter.RewardBoostDetailViewHolder>(
        RewardBoostDetailDiffCallback()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardBoostDetailViewHolder {
        val binding = ListItemBoostDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RewardBoostDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RewardBoostDetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RewardBoostDetailViewHolder(private val binding: ListItemBoostDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BoostDetailInfo) {
            binding.topDivider.visible(absoluteAdapterPosition == 0)
            binding.key.text = item.title
            binding.value.text = item.value
        }
    }
}

class RewardBoostDetailDiffCallback : DiffUtil.ItemCallback<BoostDetailInfo>() {

    override fun areItemsTheSame(
        oldItem: BoostDetailInfo,
        newItem: BoostDetailInfo
    ): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: BoostDetailInfo,
        newItem: BoostDetailInfo
    ): Boolean {
        return oldItem.title == newItem.title && oldItem.value == newItem.value
    }
}
