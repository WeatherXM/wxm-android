package com.weatherxm.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.github.mikephil.charting.charts.BarChart
import com.weatherxm.R
import com.weatherxm.databinding.ViewBarChartBinding
import com.weatherxm.ui.common.hide

class BarChartView : LinearLayout {

    private lateinit var binding: ViewBarChartBinding

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

    @SuppressLint("SetTextI18n")
    private fun init(context: Context?, attrs: AttributeSet? = null) {
        binding = ViewBarChartBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.BarChartView, 0, 0).apply {
            try {
                binding.chartTitle.text = getString(R.styleable.BarChartView_bar_chart_title)
                binding.chartTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    getResourceId(R.styleable.BarChartView_bar_chart_title_icon, 0), 0, 0, 0
                )

                getString(R.styleable.BarChartView_bar_chart_primary_line_name)?.let {
                    binding.primaryLineName.text = it
                } ?: kotlin.run {
                    binding.primaryLineName.hide(null)
                    binding.primaryLineColor.hide(null)
                }

                getString(R.styleable.BarChartView_bar_chart_secondary_line_name)?.let {
                    binding.secondaryLineName.text = it
                } ?: kotlin.run {
                    binding.secondaryLineName.hide(null)
                    binding.secondaryLineColor.hide(null)
                }

                getString(R.styleable.BarChartView_bar_chart_primary_highlight_name)?.let {
                    binding.primaryDataName.text = "$it:"
                } ?: kotlin.run {
                    binding.primaryDataName.hide(null)
                }

                getString(R.styleable.BarChartView_bar_chart_secondary_highlight_name)?.let {
                    binding.secondaryDataName.text = "$it:"
                } ?: kotlin.run {
                    binding.secondaryDataName.hide(null)
                    binding.divider.hide(null)
                }
            } finally {
                recycle()
            }
        }
    }

    fun getChart(): BarChart = binding.chart

    fun onHighlightedData(time: String, primaryData: String, secondaryData: String = "") {
        binding.time.text = time
        binding.primaryDataValue.text = primaryData
        binding.secondaryDataValue.text = secondaryData
    }

    fun onClearHighlight() {
        binding.time.text = ""
        binding.primaryDataValue.text = ""
        binding.secondaryDataValue.text = ""
    }
}
