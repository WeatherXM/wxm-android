package com.weatherxm.util

import android.icu.text.CompactDecimalFormat
import android.icu.text.NumberFormat
import com.weatherxm.util.Weather.EMPTY_VALUE
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object NumberUtils : KoinComponent {
    private val compactDecimalFormat: CompactDecimalFormat by inject()
    private val numberFormat: NumberFormat by inject()

    fun compactNumber(number: Number?): String {
        return number?.let {
            compactDecimalFormat.format(number)
        } ?: EMPTY_VALUE
    }

    fun formatNumber(number: Number?): String {
        return number?.let {
            numberFormat.format(it)
        } ?: EMPTY_VALUE
    }
}


