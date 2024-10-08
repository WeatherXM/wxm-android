package com.weatherxm.util

import androidx.annotation.ColorRes
import com.weatherxm.R
import com.weatherxm.ui.common.AnnotationGroupCode

object Rewards {

    @Suppress("MagicNumber")
    @ColorRes
    fun getRewardScoreColor(score: Int?): Int {
        return score?.let {
            when {
                it >= 80 -> R.color.success
                it >= 20 -> R.color.warning
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
                it >= 80 -> R.drawable.ic_checkmark_hex_filled
                it >= 20 -> R.drawable.ic_warning_hex_filled
                it >= 0 -> R.drawable.ic_error_hex_filled
                else -> R.drawable.ic_warning_hex_filled
            }
        } ?: R.drawable.ic_warning_hex_filled
    }

    fun AnnotationGroupCode?.isPoL(): Boolean {
        return listOf(
            AnnotationGroupCode.LOCATION_NOT_VERIFIED,
            AnnotationGroupCode.NO_LOCATION_DATA,
            AnnotationGroupCode.USER_RELOCATION_PENALTY
        ).contains(this)
    }
}
