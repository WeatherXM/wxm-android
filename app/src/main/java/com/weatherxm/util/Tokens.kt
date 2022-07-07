package com.weatherxm.util

import androidx.annotation.ColorRes
import com.weatherxm.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

object Tokens {
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
        } ?: formatTokens(0F)
    }

    fun formatTokens(amount: Float): String {
        val decimalFormat = DecimalFormat("0.00")
        // In order to match the rounding mode on roundTokens and fix inconsistencies
        decimalFormat.roundingMode = RoundingMode.HALF_UP
        return decimalFormat.format(amount.toBigDecimal())
    }

    fun roundTokens(value: Number, decimals: Int = 2): Float {
        return value.toFloat().toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP).toFloat()
    }
}
