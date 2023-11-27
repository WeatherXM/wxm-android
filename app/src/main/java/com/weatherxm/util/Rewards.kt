package com.weatherxm.util

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.weatherxm.R
import com.weatherxm.data.DeviceProfile
import com.weatherxm.ui.common.AnnotationCode
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIRewardsAnnotation
import java.math.RoundingMode
import java.text.DecimalFormat

object Rewards {
    const val REWARDS_WARNING_LIMIT = 70

    @Suppress("MagicNumber")
    @ColorRes
    fun getRewardScoreColor(score: Int?): Int {
        return score?.let {
            when {
                it >= 80 -> R.color.reward_score_very_high
                it >= 60 -> R.color.reward_score_high
                it >= 40 -> R.color.reward_score_average
                it >= 20 -> R.color.reward_score_low
                it >= 0 -> R.color.reward_score_very_low
                else -> R.color.reward_score_unknown
            }
        } ?: R.color.reward_score_unknown
    }

    fun formatTokens(amount: Float): String {
        val decimalFormat = DecimalFormat("0.00")
        decimalFormat.roundingMode = RoundingMode.HALF_UP
        return decimalFormat.format(amount.toBigDecimal())
    }

    fun getRewardAnnotationBackgroundColor(rewardScore: Int?): Int {
        return rewardScore?.let {
            if (it >= REWARDS_WARNING_LIMIT) R.color.warningTint else R.color.errorTint
        } ?: R.color.errorTint
    }

    fun getRewardAnnotationColor(rewardScore: Int?): Int {
        return rewardScore?.let {
            if (it >= REWARDS_WARNING_LIMIT) R.color.warning else R.color.error
        } ?: R.color.error
    }

    fun formatLostRewards(lostRewards: Float?): String {
        return lostRewards?.let {
            formatTokens(it)
        } ?: "?"
    }

    @Suppress("CyclomaticComplexMethod")
    @StringRes
    fun AnnotationCode.getTitleResId(): Int {
        return when (this) {
            AnnotationCode.OBC -> R.string.annotation_obc
            AnnotationCode.SPIKE_INST -> R.string.annotation_spikes
            AnnotationCode.NO_DATA -> R.string.annotation_no_data
            AnnotationCode.NO_MEDIAN -> R.string.annotation_no_median
            AnnotationCode.SHORT_CONST -> R.string.annotation_short_const
            AnnotationCode.LONG_CONST -> R.string.annotation_long_const
            AnnotationCode.FROZEN_SENSOR -> R.string.annotation_frozen_sensor
            AnnotationCode.ANOMALOUS_INCREASE -> R.string.annotation_anom_increase
            AnnotationCode.LOCATION_NOT_VERIFIED -> R.string.annotation_location_not_verified
            AnnotationCode.NO_LOCATION_DATA -> R.string.annotation_no_location
            AnnotationCode.NO_WALLET -> R.string.wallet_address_missing
            AnnotationCode.CELL_CAPACITY_REACHED -> R.string.annotation_cell_capacity_reached
            AnnotationCode.RELOCATED -> R.string.annotation_relocated
            AnnotationCode.POL_THRESHOLD_NOT_REACHED -> R.string.annotation_pol_threshold
            AnnotationCode.QOD_THRESHOLD_NOT_REACHED -> R.string.annotation_qod_threshold
            AnnotationCode.UNKNOWN -> R.string.annotation_unknown
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun UIRewardsAnnotation.getMessage(context: Context, device: UIDevice): String {
        return when (this.annotation) {
            AnnotationCode.OBC -> {
                context.getString(R.string.annotation_obc_desc, getAffectedParameters())
            }
            AnnotationCode.SPIKE_INST -> {
                if (device.relation == DeviceRelation.OWNED) {
                    context.getString(R.string.annotation_spikes_desc, getAffectedParameters())
                } else {
                    context.getString(
                        R.string.annotation_spikes_public_desc, getAffectedParameters()
                    )
                }
            }
            AnnotationCode.NO_DATA -> {
                if (device.relation == DeviceRelation.OWNED) {
                    context.getString(R.string.annotation_no_data_desc)
                } else {
                    context.getString(R.string.annotation_no_data_public_desc)
                }
            }
            AnnotationCode.NO_MEDIAN -> {
                if (device.relation == DeviceRelation.OWNED) {
                    context.getString(R.string.annotation_no_median_desc)
                } else {
                    context.getString(R.string.annotation_no_median_public_desc)
                }
            }
            AnnotationCode.SHORT_CONST -> {
                if (device.relation == DeviceRelation.OWNED) {
                    context.getString(R.string.annotation_short_const_desc, getAffectedParameters())
                } else {
                    context.getString(
                        R.string.annotation_short_const_public_desc, getAffectedParameters()
                    )
                }
            }
            AnnotationCode.LONG_CONST -> {
                if (device.relation == DeviceRelation.OWNED) {
                    context.getString(R.string.annotation_long_const_desc, getAffectedParameters())
                } else {
                    context.getString(
                        R.string.annotation_long_const_public_desc, getAffectedParameters()
                    )
                }
            }
            AnnotationCode.FROZEN_SENSOR -> {
                context.getString(R.string.annotation_frozen_sensor_desc)
            }
            AnnotationCode.ANOMALOUS_INCREASE -> {
                if (device.relation == DeviceRelation.OWNED) {
                    context.getString(
                        R.string.annotation_anom_increase_desc, getAffectedParameters()
                    )
                } else {
                    context.getString(
                        R.string.annotation_anom_increase_public_desc, getAffectedParameters()
                    )
                }
            }
            AnnotationCode.LOCATION_NOT_VERIFIED -> {
                context.getString(R.string.annotation_location_not_verified_desc)
            }
            AnnotationCode.NO_LOCATION_DATA -> {
                if (device.relation != DeviceRelation.OWNED) {
                    context.getString(R.string.annotation_no_location_public_desc)
                } else if (device.profile == DeviceProfile.M5) {
                    context.getString(R.string.annotation_no_location_m5_desc)
                } else {
                    context.getString(R.string.annotation_no_location_helium_desc)
                }
            }
            AnnotationCode.NO_WALLET -> {
                if (device.relation == DeviceRelation.OWNED) {
                    context.getString(R.string.annotation_no_wallet_desc)
                } else {
                    context.getString(R.string.annotation_no_wallet_public_desc)
                }
            }
            AnnotationCode.CELL_CAPACITY_REACHED -> {
                if (device.relation == DeviceRelation.OWNED) {
                    context.getString(R.string.annotation_cell_capacity_reached_desc)
                } else {
                    context.getString(R.string.annotation_cell_capacity_reached_public_desc)
                }
            }
            AnnotationCode.RELOCATED -> context.getString(R.string.annotation_relocated_desc)
            AnnotationCode.POL_THRESHOLD_NOT_REACHED -> {
                if (device.relation == DeviceRelation.OWNED) {
                    context.getString(R.string.annotation_pol_threshold_desc)
                } else {
                    context.getString(R.string.annotation_pol_threshold_public_desc)
                }
            }
            AnnotationCode.QOD_THRESHOLD_NOT_REACHED -> {
                if (device.relation == DeviceRelation.OWNED) {
                    context.getString(R.string.annotation_qod_threshold_desc)
                } else {
                    context.getString(R.string.annotation_qod_threshold_public_desc)
                }
            }
            AnnotationCode.UNKNOWN -> {
                if (device.relation == DeviceRelation.OWNED) {
                    context.getString(R.string.annotation_unknown_desc)
                } else {
                    context.getString(R.string.annotation_unknown_public_desc)
                }
            }
            else -> throw NotImplementedError()
        }
    }

    fun AnnotationCode?.pointToDocsHome(): Boolean {
        return listOf(
            AnnotationCode.SPIKE_INST,
            AnnotationCode.ANOMALOUS_INCREASE,
            AnnotationCode.NO_LOCATION_DATA
        ).contains(this)
    }

    fun AnnotationCode?.pointToDocsTroubleshooting(): Boolean {
        return listOf(
            AnnotationCode.NO_MEDIAN,
            AnnotationCode.NO_DATA,
            AnnotationCode.SHORT_CONST,
            AnnotationCode.LONG_CONST
        ).contains(this)
    }
}