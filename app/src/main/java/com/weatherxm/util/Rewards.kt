package com.weatherxm.util

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.weatherxm.R
import com.weatherxm.data.DeviceProfile
import com.weatherxm.ui.common.AnnotationCode
import com.weatherxm.ui.common.AnnotationGroupCode
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIRewardsAnnotation
import com.weatherxm.ui.common.empty
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

object Rewards {
    const val DIVISOR_WEI_TO_ETH = "1000000000000000000"
    const val ETH_DECIMALS = 18

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

    fun formatTokens(amount: Float?): String {
        return amount?.let {
            formatTokens(it.toBigDecimal())
        } ?: String.empty()
    }

    fun formatTokens(amount: BigDecimal): String {
        val decimalFormat = DecimalFormat("0.00##")
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
            AnnotationCode.NO_WALLET -> R.string.annotation_no_wallet
            AnnotationCode.CELL_CAPACITY_REACHED -> R.string.annotation_cell_capacity_reached
            AnnotationCode.RELOCATED -> R.string.annotation_relocated
            AnnotationCode.POL_THRESHOLD_NOT_REACHED -> R.string.annotation_pol_threshold
            AnnotationCode.QOD_THRESHOLD_NOT_REACHED -> R.string.annotation_qod_threshold
            AnnotationCode.UNIDENTIFIED_SPIKE -> R.string.annotation_unidentified_spike
            AnnotationCode.UNIDENTIFIED_ANOMALOUS_CHANGE -> R.string.annotation_unidentified_change
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
                if (device.isOwned()) {
                    context.getString(R.string.annotation_spikes_desc, getAffectedParameters())
                } else {
                    context.getString(
                        R.string.annotation_spikes_public_desc, getAffectedParameters()
                    )
                }
            }
            AnnotationCode.NO_DATA -> {
                if (device.isOwned()) {
                    context.getString(R.string.annotation_no_data_desc)
                } else {
                    context.getString(R.string.annotation_no_data_public_desc)
                }
            }
            AnnotationCode.NO_MEDIAN -> {
                if (device.isOwned()) {
                    context.getString(R.string.annotation_no_median_desc)
                } else {
                    context.getString(R.string.annotation_no_median_public_desc)
                }
            }
            AnnotationCode.SHORT_CONST -> {
                if (device.isOwned()) {
                    context.getString(R.string.annotation_short_const_desc, getAffectedParameters())
                } else {
                    context.getString(
                        R.string.annotation_short_const_public_desc, getAffectedParameters()
                    )
                }
            }
            AnnotationCode.LONG_CONST -> {
                if (device.isOwned()) {
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
                if (device.isOwned()) {
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
                if (device.isOwned()) {
                    context.getString(R.string.annotation_no_wallet_desc)
                } else {
                    context.getString(R.string.annotation_no_wallet_public_desc)
                }
            }
            AnnotationCode.CELL_CAPACITY_REACHED -> {
                if (device.isOwned()) {
                    context.getString(R.string.annotation_cell_capacity_reached_desc)
                } else {
                    context.getString(R.string.annotation_cell_capacity_reached_public_desc)
                }
            }
            AnnotationCode.RELOCATED -> context.getString(R.string.annotation_relocated_desc)
            AnnotationCode.POL_THRESHOLD_NOT_REACHED -> {
                if (device.isOwned()) {
                    context.getString(R.string.annotation_pol_threshold_desc)
                } else {
                    context.getString(R.string.annotation_pol_threshold_public_desc)
                }
            }
            AnnotationCode.QOD_THRESHOLD_NOT_REACHED -> {
                if (device.isOwned()) {
                    context.getString(R.string.annotation_qod_threshold_desc)
                } else {
                    context.getString(R.string.annotation_qod_threshold_public_desc)
                }
            }
            AnnotationCode.UNIDENTIFIED_ANOMALOUS_CHANGE -> {
                if (device.isOwned()) {
                    context.getString(
                        R.string.annotation_unidentified_change_desc, getAffectedParameters()
                    )
                } else {
                    context.getString(
                        R.string.annotation_unidentified_change_desc_public, getAffectedParameters()
                    )
                }
            }
            AnnotationCode.UNIDENTIFIED_SPIKE -> {
                if (device.isOwned()) {
                    context.getString(
                        R.string.annotation_unidentified_spike_desc, getAffectedParameters()
                    )
                } else {
                    context.getString(
                        R.string.annotation_unidentified_spike_desc_public, getAffectedParameters()
                    )
                }
            }
            AnnotationCode.UNKNOWN -> {
                if (device.isOwned()) {
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
            AnnotationCode.NO_LOCATION_DATA,
            AnnotationCode.UNIDENTIFIED_SPIKE,
            AnnotationCode.UNIDENTIFIED_ANOMALOUS_CHANGE
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

    fun AnnotationCode?.pointToPoLPreLaunch(): Boolean {
        return listOf(
            AnnotationCode.LOCATION_NOT_VERIFIED,
            AnnotationCode.NO_LOCATION_DATA
        ).contains(this)
    }
}
