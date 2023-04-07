package com.weatherxm.util

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlin.math.roundToInt

class CustomXAxisFormatter(private val times: MutableList<String>?) : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return times?.getOrNull(value.toInt()) ?: value.toString()
    }
}

class CustomYAxisFormatter(
    private val weatherUnit: String,
    private val decimals: Int = 0
) : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return if (decimals > 0) {
            "%.${decimals}f$weatherUnit".format(value)
        } else {
            "${value.roundToInt()}$weatherUnit"
        }
    }
}


