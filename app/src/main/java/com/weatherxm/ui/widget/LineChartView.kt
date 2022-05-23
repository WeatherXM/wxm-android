package com.weatherxm.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.github.mikephil.charting.charts.LineChart
import com.weatherxm.R
import com.weatherxm.databinding.ViewLineChartBinding

class LineChartView : LinearLayout {

    private lateinit var binding: ViewLineChartBinding

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context?, attrs: AttributeSet? = null) {
        binding = ViewLineChartBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.LineChartView, 0, 0).apply {
            try {
                binding.chartTitle.text = getString(R.styleable.LineChartView_line_chart_title)
            } finally {
                recycle()
            }
        }
    }

    fun getChart(): LineChart = binding.chart
}
