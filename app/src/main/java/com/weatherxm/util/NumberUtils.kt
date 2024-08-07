package com.weatherxm.util

import android.icu.text.CompactDecimalFormat
import android.icu.text.NumberFormat
import com.weatherxm.util.Weather.EMPTY_VALUE
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.RoundingMode

object NumberUtils : KoinComponent {
    private val compactDecimalFormat: CompactDecimalFormat by inject()
    private val numberFormat: NumberFormat by inject()

    fun compactNumber(number: Number?): String {
        return number?.let {
            compactDecimalFormat.format(number)
        } ?: EMPTY_VALUE
    }

    fun formatNumber(number: Number?, maxDecimals: Int = 0): String {
        return number?.let {
            numberFormat.maximumFractionDigits = maxDecimals
            numberFormat.format(it)
        } ?: EMPTY_VALUE
    }

    fun roundToDecimals(value: Number, decimals: Int = 1): Float {
        return value.toFloat().toBigDecimal().setScale(decimals, RoundingMode.HALF_UP).toFloat()
    }

    fun roundToInt(value: Number): Int {
        return value.toFloat().toBigDecimal().setScale(0, RoundingMode.HALF_UP).toInt()
    }
}


