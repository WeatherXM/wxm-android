package com.weatherxm.util

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import com.weatherxm.util.NumberUtils.roundToDecimals
import com.weatherxm.util.NumberUtils.roundToInt
import com.weatherxm.util.Weather.EMPTY_VALUE

private const val Y_AXIS_LABEL_LENGTH = 4

class CustomXAxisFormatter(private val times: MutableList<String>?) : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return times?.getOrNull(value.toInt()) ?: EMPTY_VALUE
    }
}

class CustomYAxisFormatter(
    private val decimals: Int = 0,
    private val isAxisLeft: Boolean = true
) : ValueFormatter() {
    @Suppress("MagicNumber")
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        val label = if (decimals > 0 && value < 10000) {
            "${roundToDecimals(value, decimals)}"
        } else if (value < 10000) {
            "${roundToInt(value)}"
        } else {
            // 10000-99999
            "${roundToInt(value / 1000)}K"
        }

        return if (isAxisLeft) {
            label.padStart(Y_AXIS_LABEL_LENGTH, ' ')
        } else {
            label.padEnd(Y_AXIS_LABEL_LENGTH, ' ')
        }
    }
}
