package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.weatherxm.R
import com.weatherxm.databinding.ViewRangeSelectorBinding

class RangeSelectorView : ConstraintLayout {

    private lateinit var binding: ViewRangeSelectorBinding

    constructor(context: Context) : super(context) {
        onCreate(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        onCreate(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        onCreate(context)
    }

    private fun onCreate(context: Context) {
        binding = ViewRangeSelectorBinding.inflate(LayoutInflater.from(context), this)
    }

    fun checkWeek() = binding.chartRangeSelector.check(R.id.week)
    fun checkMonth() = binding.chartRangeSelector.check(R.id.month)
    fun checkYear() = binding.chartRangeSelector.check(R.id.year)

    fun checkedChipId() = binding.chartRangeSelector.checkedChipId

    fun listener(listener: (Int) -> Unit) {
        binding.chartRangeSelector.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                listener.invoke(checkedIds[0])
            }
        }
    }

    fun enable() {
        binding.week.isEnabled = true
        binding.month.isEnabled = true
        binding.year.isEnabled = true
    }

    fun disable() {
        binding.week.isEnabled = false
        binding.month.isEnabled = false
        binding.year.isEnabled = false
    }
}
