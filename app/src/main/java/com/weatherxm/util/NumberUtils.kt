package com.weatherxm.util

import android.icu.text.CompactDecimalFormat
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.weatherxm.data.DECIMAL_FORMAT_TOKENS
import com.weatherxm.ui.common.Contracts.EMPTY_VALUE
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat

object NumberUtils : KoinComponent {
    private val compactDecimalFormat: CompactDecimalFormat by inject()
    private val numberFormat: NumberFormat by inject()
    private val decimalFormat: DecimalFormat by inject(named(DECIMAL_FORMAT_TOKENS))

    private const val DIVISOR_WEI_TO_ETH = "1000000000000000000"
    private const val ETH_DECIMALS = 18

    fun compactNumber(number: Number?): String {
        return number?.let {
            compactDecimalFormat.format(number)
        } ?: EMPTY_VALUE
    }

    fun formatNumber(number: Number?, decimals: Int = 0): String {
        return number?.let {
            numberFormat.minimumFractionDigits = decimals
            numberFormat.maximumFractionDigits = decimals
            numberFormat.format(it)
        } ?: EMPTY_VALUE
    }

    fun formatTokens(amount: Float?): String {
        return amount?.let {
            formatTokens(it.toBigDecimalSafe())
        } ?: EMPTY_VALUE
    }

    fun formatTokens(amount: BigDecimal): String {
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

    fun roundToDecimals(value: Number, decimals: Int = 1): Float {
        return value.toFloat().toBigDecimalSafe().setScale(decimals, RoundingMode.HALF_UP).toFloat()
    }

    fun Double.toBigDecimalSafe(): BigDecimal {
        return when {
            this.isNaN() || this.isInfinite() -> BigDecimal.ZERO
            else -> try {
                this.toBigDecimal()
            } catch (e: NumberFormatException) {
                Timber.e(e)
                Firebase.crashlytics.recordException(Throwable("Invalid Number: $this", e))
                BigDecimal.ZERO
            }
        }
    }

    fun Float.toBigDecimalSafe(): BigDecimal {
        return when {
            this.isNaN() || this.isInfinite() -> BigDecimal.ZERO
            else -> try {
                this.toBigDecimal()
            } catch (e: NumberFormatException) {
                Timber.e(e)
                Firebase.crashlytics.recordException(Throwable("Invalid Number: $this", e))
                BigDecimal.ZERO
            }
        }
    }
}


