package com.weatherxm.ui.rewardslist

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemRewardBinding
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIRewardObject
import com.weatherxm.ui.common.setVisible
import com.weatherxm.util.Mask
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Rewards.formatLostRewards
import com.weatherxm.util.Rewards.formatTokens
import com.weatherxm.util.Rewards.getRewardAnnotationBackgroundColor
import com.weatherxm.util.Rewards.getRewardAnnotationColor
import com.weatherxm.util.Rewards.getRewardScoreColor
import com.weatherxm.util.setRewardStatusChip
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RewardsListAdapter(
    private val deviceRelation: DeviceRelation?,
    private val onRewardDetails: (UIRewardObject) -> Unit,
    private val onEndOfData: () -> Unit
) : ListAdapter<UIRewardObject,
    RewardsListAdapter.RewardsViewHolder>(UITransactionDiffCallback()),
    KoinComponent {

    val resHelper: ResourcesHelper by inject()

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
        private val onRewardDetails: (UIRewardObject) -> Unit,
        private val onEndOfData: () -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.rewardsCard.setOnClickListener {
                val transaction = getItem(absoluteAdapterPosition)
                onRewardDetails(transaction)
            }
        }

        @Suppress("MagicNumber")
        fun bind(item: UIRewardObject, position: Int) {
            if (position == currentList.size - 1) {
                onEndOfData()
            }

            updateDateAndLines(item.rewardFormattedDate, position)

            binding.txHash.text = item.txHash?.let {
                Mask.maskHash(hash = it, offsetStart = 8, offsetEnd = 8, maxMaskedChars = 6)
            }

            item.actualReward?.let {
                binding.reward.text = resHelper.getString(R.string.reward, formatTokens(it))
            }

            item.rewardScore?.let {
                binding.rewardStatus.setRewardStatusChip(it)
            } ?: binding.rewardStatus.setVisible(false)

            item.periodMaxReward?.let {
                binding.maxReward.text = formatTokens(it)

                with(binding.rewardSlider) {
                    valueFrom = 0.0F
                    valueTo = it
                    values = listOf(0.0F, item.actualReward)
                }
            }

            binding.timestamp.text = item.rewardFormattedTimestamp

            binding.score.text = item.rewardScore?.let {
                itemView.resources.getString(R.string.score, (it.toFloat() / 100))
            } ?: itemView.resources.getString(R.string.score_unknown)

            with(binding.scoreIcon) {
                val color = context.getColor(getRewardScoreColor(item.rewardScore))
                setColorFilter(color)
                binding.rewardSlider.trackActiveTintList = ColorStateList.valueOf(color)
            }

            if (item.annotations.isNotEmpty() || ((item.rewardScore) ?: 0) < 100) {
                binding.mainCard.strokeColor =
                    itemView.context.getColor(getRewardAnnotationColor(item.rewardScore))
                binding.mainCard.strokeWidth = 2
                setErrorData(item, onRewardDetails)
            } else {
                binding.mainCard.strokeWidth = 0
                binding.problemsCard.setVisible(false)
            }
        }

        /*
        * Responsible for grouping transactions and for hiding the prevLine on the first item of the
        * list.
        *
        * Grouping is done by date, which means we check if the previous transaction has the same
        * date as this one, then we hide the date and the respective line views.
         */
        private fun updateDateAndLines(formattedDate: String?, position: Int) {
            binding.prevLine.visibility = if (position == 0) View.GONE else View.VISIBLE

            if (position == 0) {
                binding.prevLine.visibility = View.GONE
                binding.date.text = formattedDate
            } else {
                val prevTx = getItem(position - 1)

                if (formattedDate == prevTx.rewardFormattedDate) {
                    binding.prevLine.visibility = View.GONE
                    binding.datePoint.visibility = View.GONE
                    binding.date.visibility = View.GONE
                } else {
                    binding.prevLine.visibility = View.VISIBLE
                    binding.datePoint.visibility = View.VISIBLE
                    binding.date.visibility = View.VISIBLE
                    binding.date.text = formattedDate
                }
            }
        }

        private fun setErrorData(data: UIRewardObject, onProblems: (UIRewardObject) -> Unit) {
            with(binding.problemsCard) {
                val actionMessage = context.getString(
                    if (deviceRelation == DeviceRelation.OWNED) {
                        R.string.identify_fix_problems
                    } else {
                        R.string.see_detailed_problems
                    }
                )
                val backgroundColorResId = getRewardAnnotationBackgroundColor(data.rewardScore)
                setBackground(backgroundColorResId)
                setBackgroundColor(context.getColor(backgroundColorResId))
                action(actionMessage) {
                    onProblems.invoke(data)
                }
                if (((data.lostRewards) ?: 0F) == 0F && (data.periodMaxReward ?: 0F) != 0F) {
                    message(R.string.problems_found_desc_without_lost_rewards)
                } else {
                    val lostRewards = formatLostRewards(data.lostRewards)
                    htmlMessage(context.getString(R.string.problems_found_desc, lostRewards))
                }
                setVisible(true)
            }
        }
    }

    class UITransactionDiffCallback : DiffUtil.ItemCallback<UIRewardObject>() {

        override fun areItemsTheSame(oldItem: UIRewardObject, newItem: UIRewardObject): Boolean {
            return oldItem.txHash == newItem.txHash
        }

        override fun areContentsTheSame(oldItem: UIRewardObject, newItem: UIRewardObject): Boolean {
            return oldItem.txHash == newItem.txHash &&
                oldItem.rewardTimestamp == newItem.rewardTimestamp &&
                oldItem.rewardFormattedDate == newItem.rewardFormattedDate &&
                oldItem.rewardScore == newItem.rewardScore &&
                oldItem.periodMaxReward == newItem.periodMaxReward &&
                oldItem.lostRewards == newItem.lostRewards &&
                oldItem.actualReward == newItem.actualReward
        }
    }
}
