package com.weatherxm.ui.rewardslist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.data.Reward
import com.weatherxm.databinding.ListItemRewardBinding
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.setVisible
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.Resources
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.ZonedDateTime

class RewardsListAdapter(
    private val onRewardDetails: (Reward) -> Unit,
    private val onEndOfData: () -> Unit
) : ListAdapter<Reward, RewardsListAdapter.RewardsViewHolder>(RewardDiffCallback()), KoinComponent {

    val resources: Resources by inject()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardsViewHolder {
        val binding = ListItemRewardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RewardsViewHolder(binding, onRewardDetails, onEndOfData)
    }

    override fun onBindViewHolder(holder: RewardsViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class RewardsViewHolder(
        private val binding: ListItemRewardBinding,
        private val onRewardDetails: (Reward) -> Unit,
        private val onEndOfData: () -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.mainCard.setOnClickListener {
                val transaction = getItem(absoluteAdapterPosition)
                onRewardDetails(transaction)
            }
        }

        @Suppress("MagicNumber")
        fun bind(item: Reward, position: Int) {
            if (position == currentList.size - 1) {
                onEndOfData()
            }
            if (!item.isEmpty()) {
                updateDateAndLines(item.timestamp, position)
                binding.mainCard.updateUI(item, true)
            } else {
                binding.date.setVisible(false)
                binding.mainCard.setVisible(false)
                binding.endOfDataCard.setVisible(true)
            }
        }

        /**
         * Responsible for grouping transactions and
         * for hiding the prevLine on the first item of the list.
         *
         * Grouping is done by date, which means we check if the previous transaction has the same
         * date as this one, then we hide the date and the respective line views.
         */
        private fun updateDateAndLines(timestamp: ZonedDateTime?, position: Int) {
            val formattedDate = timestamp.getFormattedDate(true)

            if (position == 0) {
                binding.prevLine.setVisible(false)
                binding.date.text = formattedDate
            } else {
                val prevFormattedDate =
                    getItem(position - 1).timestamp?.getFormattedDate(true) ?: String.empty()

                if (formattedDate == prevFormattedDate) {
                    binding.prevLine.setVisible(false)
                    binding.datePoint.setVisible(false)
                    binding.date.setVisible(false)
                } else {
                    binding.prevLine.setVisible(true)
                    binding.datePoint.setVisible(true)
                    binding.date.setVisible(true)
                    binding.date.text = formattedDate
                }
            }
        }
    }

    class RewardDiffCallback : DiffUtil.ItemCallback<Reward>() {

        override fun areItemsTheSame(oldItem: Reward, newItem: Reward): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: Reward, newItem: Reward): Boolean {
            return oldItem.timestamp == newItem.timestamp &&
                oldItem.baseReward == newItem.baseReward &&
                oldItem.baseRewardScore == newItem.baseRewardScore &&
                oldItem.totalBoostReward == newItem.totalBoostReward &&
                oldItem.totalReward == newItem.totalReward &&
                oldItem.annotationSummary?.size == newItem.annotationSummary?.size
        }
    }
}
