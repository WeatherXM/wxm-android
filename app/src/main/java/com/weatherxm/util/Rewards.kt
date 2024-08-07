package com.weatherxm.util

import androidx.annotation.ColorRes
import com.weatherxm.R
import com.weatherxm.ui.common.AnnotationGroupCode
import com.weatherxm.ui.common.empty
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

object Rewards {
    private const val DIVISOR_WEI_TO_ETH = "1000000000000000000"
    private const val ETH_DECIMALS = 18

    @Suppress("MagicNumber")
    @ColorRes
    fun getRewardScoreColor(score: Int?): Int {
        return score?.let {
            when {
                it >= 95 -> R.color.green
                it >= 10 -> R.color.warning
                it >= 0 -> R.color.error
                else -> R.color.reward_score_unknown
            }
        } ?: R.color.reward_score_unknown
    }

    @Suppress("MagicNumber")
    @ColorRes
    fun getRewardIcon(score: Int?): Int {
        return score?.let {
            when {
                it >= 95 -> R.drawable.ic_checkmark_hex_filled
                it >= 10 -> R.drawable.ic_warning_hex_filled
                it >= 0 -> R.drawable.ic_error_hex_filled
                else -> R.drawable.ic_warning_hex_filled
            }
        } ?: R.drawable.ic_warning_hex_filled
    }

    fun formatTokens(amount: Float?): String {
        return amount?.let {
            formatTokens(it.toBigDecimal())
        } ?: String.empty()
    }

    fun formatTokens(amount: BigDecimal): String {
        val decimalFormat = DecimalFormat("0.00")
        decimalFormat.roundingMode = RoundingMode.HALF_UP
        return decimalFormat.format(amount)
    }

    @Suppress("MagicNumber")
    fun weiToETH(amount: BigDecimal): BigDecimal {
        /**
         * Mandatory otherwise we get a result of 0E-18 instead of 0
         */
        if (amount == BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }
        return amount.divide(BigDecimal(DIVISOR_WEI_TO_ETH), ETH_DECIMALS, RoundingMode.HALF_UP)
    }

    fun AnnotationGroupCode?.isPoL(): Boolean {
        return listOf(
            AnnotationGroupCode.LOCATION_NOT_VERIFIED,
            AnnotationGroupCode.NO_LOCATION_DATA,
            AnnotationGroupCode.USER_RELOCATION_PENALTY
        ).contains(this)
    }
}
