package com.weatherxm.ui.token

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.Transaction
import com.weatherxm.databinding.ListItemTokenTransactionCardBinding
import com.weatherxm.util.Mask
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.getRelativeDayFromISO
import com.weatherxm.util.setTextAndColor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TransactionsAdapter(private val transactionListener: (Transaction) -> Unit) :
    ListAdapter<Transaction, TransactionsAdapter.TransactionsViewHolder>(TransactionDiffCallback()),
    KoinComponent {
    companion object {
        const val BAD_SCORE = 0.25
        const val OK_SCORE = 0.5
        const val GOOD_SCORE = 0.75
    }

    val resHelper: ResourcesHelper by inject()
    val mask: Mask by inject()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionsViewHolder {
        val binding = ListItemTokenTransactionCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionsViewHolder(binding, transactionListener)
    }

    override fun onBindViewHolder(holder: TransactionsViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class TransactionsViewHolder(
        private val binding: ListItemTokenTransactionCardBinding,
        private val listener: (Transaction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var transaction: Transaction

        init {
            binding.root.setOnClickListener {
                listener(transaction)
            }
        }

        fun bind(item: Transaction, position: Int) {
            transaction = item

            if (position == itemCount - 1) {
                binding.prevLine.visibility = View.GONE
            } else if (position == 0) {
                binding.nextLine.visibility = View.GONE
            }

            binding.date.text =
                item.timestamp?.let {
                    getRelativeDayFromISO(
                        resHelper, it,
                        includeDate = true,
                        fullName = false
                    )
                }
            binding.txHash.text =
                item.txHash?.let {
                    resHelper.getString(R.string.transaction_hash, mask.maskHash(it))
                }
            binding.reward.text = resHelper.getString(
                R.string.reward,
                item.actualReward.toString()
            )
            binding.maxReward.text = resHelper.getString(
                R.string.max_reward,
                item.dailyReward.toString()
            )

            item.validationScore?.let {
                when {
                    it >= GOOD_SCORE -> binding.scale.setTextAndColor(it.toString(), R.color.green)
                    it >= OK_SCORE -> binding.scale.setTextAndColor(it.toString(), R.color.yellow)
                    it >= BAD_SCORE -> binding.scale.setTextAndColor(it.toString(), R.color.orange)
                    else -> binding.scale.setTextAndColor(it.toString(), R.color.red)
                }
            }
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
                oldItem.totalRewards == newItem.totalRewards &&
                oldItem.lostRewards == newItem.lostRewards &&
                oldItem.wxmBalance == newItem.wxmBalance
        }
    }
}
