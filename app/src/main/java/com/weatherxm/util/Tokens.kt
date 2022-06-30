package com.weatherxm.util

import androidx.annotation.ColorRes
import com.weatherxm.R
import java.math.BigDecimal
import java.text.DecimalFormat

object Tokens {
    private const val EMPTY_VALUE = "-"

    @Suppress("MagicNumber")
    @ColorRes
    fun getRewardScoreColor(score: Float?): Int {
        return score?.let {
            when {
                it >= 0.8F -> R.color.reward_score_very_high
                it >= 0.6F -> R.color.reward_score_high
                it >= 0.4F -> R.color.reward_score_average
                it >= 0.2F -> R.color.reward_score_low
                it >= 0.0F -> R.color.reward_score_very_low
                else -> R.color.reward_score_unknown
            }
        } ?: R.color.reward_score_unknown
    }

    fun formatValue(value: Float?): String {
        return value?.let {
            formatTokens(it)
        } ?: EMPTY_VALUE
    }

    fun formatTokens(amount: Float): String {
        return DecimalFormat("0.00").format(amount.toBigDecimal())
    }

    fun roundTokens(value: Number, decimals: Int = 2): Float {
        return value.toFloat().toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP).toFloat()
    }
}
