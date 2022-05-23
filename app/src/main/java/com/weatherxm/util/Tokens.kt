package com.weatherxm.util

import androidx.annotation.ColorRes
import com.weatherxm.R
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

    fun formatTokens(amount: Float): String {
        return DecimalFormat("0.00").format(amount.toBigDecimal())
    }
}
