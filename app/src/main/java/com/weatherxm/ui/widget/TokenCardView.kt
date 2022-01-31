package com.weatherxm.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.databinding.ViewTokenCardBinding
import com.weatherxm.ui.TokenSummary
import com.weatherxm.ui.userdevice.UserDeviceViewModel

open class TokenCardView : LinearLayout {

    private lateinit var binding: ViewTokenCardBinding
    private lateinit var tokenSummary: TokenSummary

    var optionListener: TokenOptionListener? = null

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context?) {
        binding = ViewTokenCardBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER

        binding.tokenOptions.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.token24h -> {
                    optionListener?.onOptionClick(UserDeviceViewModel.TokensState.HOUR24)
                }
                R.id.token7d -> {
                    optionListener?.onOptionClick(UserDeviceViewModel.TokensState.DAYS7)
                }
                R.id.token30d -> {
                    optionListener?.onOptionClick(UserDeviceViewModel.TokensState.DAYS30)
                }
            }
        }
    }

    fun setTokenData(data: TokenSummary) {
        tokenSummary = data
        chart(data.values)
        total(data.total)
    }

    private fun chart(data: List<Pair<String, Float>>?) {
        if (!data.isNullOrEmpty()) {
            //  Necessary fix for a crash, found the fix on library's BarChartView.kt line 85
            binding.tokenChart.barsColorsList =
                List(data.size) { binding.tokenChart.barsColor }.toList()
            binding.tokenChart.show(data)
        }
    }

    private fun total(total: Float?) {
        total?.let {
            binding.wxmValue.text = it.toString()
        }
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }

    interface TokenOptionListener {
        fun onOptionClick(tokenOption: UserDeviceViewModel.TokensState)
    }
}
