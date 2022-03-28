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
import com.weatherxm.util.getRelativeDayFromISO
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import kotlin.math.roundToInt

class TransactionsAdapter(
    private val transactionListener: (Transaction) -> Unit,
    private val endOfDataListener: () -> Unit
) : ListAdapter<Transaction, TransactionsAdapter.TransactionsViewHolder>(TransactionDiffCallback()),
    KoinComponent {
    companion object {
        const val BAD_SCORE = 0.25
        const val OK_SCORE = 0.5
        const val GOOD_SCORE = 0.75
        const val MULTIPLY_WITH_FOR_PERCENT = 100
    }

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
            if (position == currentList.size - 1) endOfDataListener()
            transaction = item

            binding.prevLine.visibility = if (position == 0) View.GONE else View.VISIBLE

            item.timestamp?.let {
                binding.relativeDate.text = getRelativeDayFromISO(
                    resHelper, it,
                    includeDate = false,
                    fullName = false
                )
                binding.date.text = ZonedDateTime.parse(it).format(dateFormat)
            }
            binding.txHash.text = item.txHash?.let {
                mask.maskHash(hash = it, offsetStart = 8, offsetEnd = 8, maxMaskedChars = 6)
            }
            binding.reward.text = resHelper.getString(
                R.string.reward,
                item.actualReward.toString()
            )
            item.actualReward?.let {
                binding.rewardSlider.apply {
                    valueFrom = 0F
                    valueTo = it
                    values = listOf(0F, it)
                }
            }

            item.dailyReward?.let {
                binding.maxReward.text = it.toString()
                binding.maxRewardSlider.apply {
                    valueFrom = 0F
                    valueTo = it
                    values = listOf(0F, it)
                }
            }

            item.validationScore?.let {
                val percentage = (it * MULTIPLY_WITH_FOR_PERCENT).roundToInt().toString()
                binding.score.text = resHelper.getString(R.string.score, percentage)
                when {
                    it >= GOOD_SCORE -> {
                        binding.scoreIcon.setColorFilter(resHelper.getColor(R.color.green))
                        binding.rewardSlider.trackActiveTintList =
                            ColorStateList.valueOf(resHelper.getColor(R.color.green))
                    }
                    it >= OK_SCORE -> {
                        binding.scoreIcon.setColorFilter(resHelper.getColor(R.color.yellow))
                        binding.rewardSlider.trackActiveTintList =
                            ColorStateList.valueOf(resHelper.getColor(R.color.yellow))
                    }
                    it >= BAD_SCORE -> {
                        binding.scoreIcon.setColorFilter(resHelper.getColor(R.color.orange))
                        binding.rewardSlider.trackActiveTintList =
                            ColorStateList.valueOf(resHelper.getColor(R.color.orange))
                    }
                    else -> {
                        binding.scoreIcon.setColorFilter(resHelper.getColor(R.color.red))
                        binding.rewardSlider.trackActiveTintList =
                            ColorStateList.valueOf(resHelper.getColor(R.color.red))
                    }
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
                oldItem.wxmBalance == newItem.wxmBalance
        }
    }
}
