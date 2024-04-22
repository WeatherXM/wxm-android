package com.weatherxm.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.github.mikephil.charting.charts.LineChart
import com.weatherxm.R
import com.weatherxm.databinding.ViewLineChartBinding
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.setVisible

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

    @SuppressLint("SetTextI18n")
    private fun init(context: Context?, attrs: AttributeSet? = null) {
        binding = ViewLineChartBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.LineChartView, 0, 0).apply {
            try {
                binding.chartTitle.text = getString(R.styleable.LineChartView_line_chart_title)
                binding.chartTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    getResourceId(R.styleable.LineChartView_line_chart_title_icon, 0), 0, 0, 0
                )

                getString(R.styleable.LineChartView_line_chart_primary_line_name)?.let {
                    binding.primaryLineName.text = it
                } ?: kotlin.run {
                    binding.primaryLineName.setVisible(false)
                    binding.primaryLineColor.setVisible(false)
                }

                getString(R.styleable.LineChartView_line_chart_secondary_line_name)?.let {
                    binding.secondaryLineName.text = it
                } ?: kotlin.run {
                    binding.secondaryLineName.setVisible(false)
                    binding.secondaryLineColor.setVisible(false)
                }

                getString(R.styleable.LineChartView_line_chart_primary_highlight_name)?.let {
                    binding.primaryDataName.text = it
                } ?: kotlin.run {
                    binding.primaryDataName.setVisible(false)
                }

                getString(R.styleable.LineChartView_line_chart_secondary_highlight_name)?.let {
                    binding.secondaryDataName.text = it
                } ?: kotlin.run {
                    binding.secondaryDataName.setVisible(false)
                    binding.divider.setVisible(false)
                }

                binding.secondaryLineColor.setChipBackgroundColorResource(
                    getResourceId(
                        R.styleable.LineChartView_line_chart_secondary_line_color,
                        R.color.chart_secondary_line
                    )
                )
            } finally {
                recycle()
            }
        }
    }

    fun getChart(): LineChart = binding.chart

    fun getDatasetsSize(): Int {
        return if (binding.chart.data == null) 0 else binding.chart.data.dataSets.size
    }

    fun onHighlightValue(x: Float, dataSetIndex: Int?) {
        if (binding.chart.data == null || dataSetIndex == null) return
        binding.chart.highlightValue(x, dataSetIndex)
    }

    fun onHighlightedData(
        time: String,
        primaryData: String,
        secondaryData: String = String.empty()
    ) {
        binding.time.text = time
        binding.primaryDataValue.text = primaryData
        binding.secondaryDataValue.text = secondaryData
    }

    fun onClearHighlight() {
        binding.time.text = String.empty()
        binding.primaryDataValue.text = String.empty()
        binding.secondaryDataValue.text = String.empty()
    }

    fun clearChart() {
        onClearHighlight()
        getChart().highlightValue(null)
        getChart().clear()
    }

    fun showNoDataText() {
        binding.chart.setNoDataText(resources.getString(R.string.error_history_no_data_chart_found))
        binding.chart.setNoDataTextColor(context.getColor(R.color.colorOnSurface))
        binding.chart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD)
    }

    fun updateTitle(text: String) {
        binding.chartTitle.text = text
    }

    fun primaryLine(lineName: String?, dataName: String) {
        binding.primaryLineName.setVisible(!lineName.isNullOrEmpty())
        binding.primaryLineColor.setVisible(!lineName.isNullOrEmpty())
        binding.primaryDataName.text = dataName
        binding.primaryLineName.text = lineName
    }

    fun secondaryLine(lineName: String?, dataName: String?) {
        binding.secondaryLineName.setVisible(!lineName.isNullOrEmpty())
        binding.secondaryLineColor.setVisible(!lineName.isNullOrEmpty())
        binding.secondaryDataName.setVisible(!dataName.isNullOrEmpty())
        binding.divider.setVisible(!dataName.isNullOrEmpty())
        binding.secondaryDataName.text = dataName
        binding.secondaryLineName.text = lineName
    }
}
