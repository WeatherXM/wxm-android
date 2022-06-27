package com.weatherxm.ui.token

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.Transaction
import com.weatherxm.databinding.ListItemTokenTransactionBinding
import com.weatherxm.util.Mask
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Tokens.formatTokens
import com.weatherxm.util.Tokens.getRewardScoreColor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

class TransactionsAdapter(
    private val transactionListener: (Transaction) -> Unit,
    private val endOfDataListener: () -> Unit
) : ListAdapter<Transaction,
    TransactionsAdapter.TransactionsViewHolder>(TransactionDiffCallback()),
    KoinComponent {

    val resHelper: ResourcesHelper by inject()
    val mask: Mask by inject()

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
        private val listener: (Transaction) -> Unit,
        private val endOfDataListener: () -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var transaction: Transaction
        private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(MEDIUM)

        init {
            binding.card.setOnClickListener {
                listener(transaction)
            }
        }

        fun bind(item: Transaction, position: Int) {
            if (position == currentList.size - 1) {
                endOfDataListener()
            }

            transaction = item

            binding.prevLine.visibility = if (position == 0) View.GONE else View.VISIBLE

            binding.date.text = ZonedDateTime.parse(item.timestamp).format(dateFormat)

            binding.txHash.text = item.txHash?.let {
                mask.maskHash(hash = it, offsetStart = 8, offsetEnd = 8, maxMaskedChars = 6)
            }

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

            binding.score.text = item.validationScore?.let {
                itemView.resources.getString(R.string.score, it)
            } ?: itemView.resources.getString(R.string.score_unknown)

            val color = resHelper.getColor(getRewardScoreColor(item.validationScore))
            binding.scoreIcon.setColorFilter(color)
            binding.rewardSlider.trackActiveTintList = ColorStateList.valueOf(color)
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {

        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.txHash == newItem.txHash
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.txHash == newItem.txHash &&
                oldItem.timestamp == newItem.timestamp &&
                oldItem.validationScore == newItem.validationScore &&
                oldItem.dailyReward == newItem.dailyReward &&
                oldItem.actualReward == newItem.actualReward &&
                oldItem.totalRewards == newItem.totalRewards
        }
    }
}
