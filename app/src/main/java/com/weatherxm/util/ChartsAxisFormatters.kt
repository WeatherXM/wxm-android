package com.weatherxm.util

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import com.weatherxm.ui.common.Contracts.EMPTY_VALUE
import com.weatherxm.util.NumberUtils.formatNumber

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
        val label = if (value < 10000) {
            formatNumber(value, decimals)
        } else {
            // 10000-99999
            "${formatNumber(value / 1000, 0)}K"
        }

        return if (isAxisLeft) {
            label.padStart(Y_AXIS_LABEL_LENGTH, ' ')
        } else {
            label.padEnd(Y_AXIS_LABEL_LENGTH, ' ')
        }
    }
}
