package com.weatherxm.ui.rewardslist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.data.Reward
import com.weatherxm.databinding.ListItemRewardBinding
import com.weatherxm.databinding.ListItemRewardEndOfDataBinding
import com.weatherxm.ui.common.RewardTimelineType
import com.weatherxm.ui.common.TimelineReward
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.visible
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.Resources
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.ZonedDateTime

class RewardsListAdapter(
    private val onRewardDetails: (Reward) -> Unit,
    private val onEndOfData: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), KoinComponent {

    private val adapterData = mutableListOf<TimelineReward>()

    val resources: Resources by inject()

    fun setData(data: List<TimelineReward>) {
        adapterData.apply {
            clear()
            addAll(data)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                val binding = ListItemRewardBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                RewardsViewHolder(binding, onRewardDetails, onEndOfData)
            }
            1 -> {
                val bindingEndOfData = ListItemRewardEndOfDataBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                RewardsEndOfDataViewHolder(bindingEndOfData)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = adapterData[position]
        if (item.type == RewardTimelineType.DATA && item.data != null) {
            (holder as RewardsViewHolder).bind(item.data, position)
        }
    }

    override fun getItemCount(): Int = adapterData.size

    override fun getItemViewType(position: Int): Int {
        return if (adapterData[position].type == RewardTimelineType.DATA) {
            TYPE_REWARD
        } else {
            TYPE_END_OF_DATA
        }
    }

    inner class RewardsViewHolder(
        private val binding: ListItemRewardBinding,
        private val onRewardDetails: (Reward) -> Unit,
        private val onEndOfData: () -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Reward, position: Int) {
            if (position == adapterData.size - 1) {
                onEndOfData()
            }
            updateDateAndLines(item.timestamp, position)
            binding.mainCard.setOnClickListener {
                onRewardDetails(item)
            }
            binding.mainCard.updateUI(item, true)
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
                binding.prevLine.visible(false)
                binding.date.text = formattedDate
            } else {
                val prevFormattedDate =
                    adapterData[position - 1].data?.timestamp?.getFormattedDate(true)
                        ?: String.empty()

                if (formattedDate == prevFormattedDate) {
                    binding.prevLine.visible(false)
                    binding.datePoint.visible(false)
                    binding.date.visible(false)
                } else {
                    binding.prevLine.visible(true)
                    binding.datePoint.visible(true)
                    binding.date.visible(true)
                    binding.date.text = formattedDate
                }
            }
        }
    }

    inner class RewardsEndOfDataViewHolder(
        binding: ListItemRewardEndOfDataBinding
    ) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val TYPE_REWARD = 0
        private const val TYPE_END_OF_DATA = 1
    }
}
