package com.weatherxm.ui.token

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemTokenTransactionBinding
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Tokens.formatTokens
import com.weatherxm.util.Tokens.getRewardScoreColor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TransactionsAdapter(
    private val transactionListener: (UITransaction) -> Unit,
    private val endOfDataListener: () -> Unit
) : ListAdapter<UITransaction,
    TransactionsAdapter.TransactionsViewHolder>(UITransactionDiffCallback()),
    KoinComponent {

    val resHelper: ResourcesHelper by inject()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionsViewHolder {
        val binding = ListItemTokenTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionsViewHolder(binding, transactionListener, endOfDataListener)
    }

    override fun onBindViewHolder(holder: TransactionsViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class TransactionsViewHolder(
        private val binding: ListItemTokenTransactionBinding,
        private val listener: (UITransaction) -> Unit,
        private val endOfDataListener: () -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.card.setOnClickListener {
                val transaction = getItem(absoluteAdapterPosition)
                listener(transaction)
            }
        }

        fun bind(item: UITransaction, position: Int) {
            if (position == currentList.size - 1) {
                endOfDataListener()
            }

            updateDateAndLines(item.formattedDate, position)

            binding.txHash.text = item.txHashMasked

            item.actualReward?.let {
                binding.reward.text = resHelper.getString(R.string.reward, formatTokens(it))
            }

            item.dailyReward?.let {
                binding.maxReward.text = formatTokens(it)

                with(binding.rewardSlider) {
                    valueFrom = 0.0F
                    valueTo = it
                    values = listOf(0.0F, item.actualReward)
                }
            }

            binding.timestamp.text = item.formattedTimestamp

            binding.score.text = item.validationScore?.let {
                itemView.resources.getString(R.string.score, it)
            } ?: itemView.resources.getString(R.string.score_unknown)

            val color = resHelper.getColor(getRewardScoreColor(item.validationScore))
            binding.scoreIcon.setColorFilter(color)
            binding.rewardSlider.trackActiveTintList = ColorStateList.valueOf(color)
        }

        /*
        * Responsible for grouping transactions and for hiding the prevLine on the first item of the
        * list.
        *
        * Grouping is done by date, which means we check if the previous transaction has the same
        * date as this one, then we hide the date and the respective line views.
         */
        private fun updateDateAndLines(formattedDate: String, position: Int) {
            binding.prevLine.visibility = if (position == 0) View.GONE else View.VISIBLE

            if (position == 0) {
                binding.prevLine.visibility = View.GONE
                binding.date.text = formattedDate
            } else {
                val prevTx = getItem(position - 1)

                if (formattedDate == prevTx.formattedDate) {
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
    }

    class UITransactionDiffCallback : DiffUtil.ItemCallback<UITransaction>() {

        override fun areItemsTheSame(oldItem: UITransaction, newItem: UITransaction): Boolean {
            return oldItem.txHash == newItem.txHash
        }

        override fun areContentsTheSame(oldItem: UITransaction, newItem: UITransaction): Boolean {
            return oldItem.txHash == newItem.txHash &&
                oldItem.formattedDate == newItem.formattedDate &&
                oldItem.validationScore == newItem.validationScore &&
                oldItem.dailyReward == newItem.dailyReward &&
                oldItem.actualReward == newItem.actualReward
        }
    }
}
