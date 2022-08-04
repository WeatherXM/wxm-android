package com.weatherxm.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.data.Transaction
import com.weatherxm.data.Transaction.Companion.VERY_SMALL_NUMBER_FOR_CHART
import com.weatherxm.databinding.ViewTokenCardBinding
import com.weatherxm.ui.TokenInfo
import com.weatherxm.ui.TokenValuesChart
import com.weatherxm.util.DateTimeHelper.getHourMinutesFromISO
import com.weatherxm.util.DateTimeHelper.getRelativeDayFromISO
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Tokens
import com.weatherxm.util.Tokens.formatTokens
import com.weatherxm.util.Tokens.formatValue
import com.weatherxm.util.setChildrenEnabled
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class TokenCardView : LinearLayout, KoinComponent {

    private lateinit var binding: ViewTokenCardBinding
    val resHelper: ResourcesHelper by inject()

    private var tokenInfo: TokenInfo? = null

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

        // Disable the different options initially, we enable it later after the first fetch of data
        binding.tokenOptions.setChildrenEnabled(false)

        binding.tokenOptions.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.option7D -> {
                    setChartData(tokenInfo?.chart7d, tokenInfo?.max7dReward)
                }
                R.id.option30D -> {
                    setChartData(tokenInfo?.chart30d, tokenInfo?.max30dReward)
                }
            }
        }
    }

    fun setTokenInfo(data: TokenInfo, totalAllTime: Float?) {
        this.tokenInfo = data
        updateUI(totalAllTime)
    }

    private fun updateUI(totalAllTime: Float?) {
        binding.total7DWXM.text = formatValue(tokenInfo?.total7d)
        binding.total30DWXM.text = formatValue(tokenInfo?.total30d)
        binding.tokenOptions.setChildrenEnabled(true)

        /*
        * TODO: On explorer, {totalAllTime = null} always at this point as  we don't have it yet,
        * so perform this simple check to hide the total info when it's being used on explorer
         */
        if (totalAllTime == null) {
            binding.totalAll.visibility = View.GONE
            binding.totalAllWXM.visibility = View.GONE
        } else {
            binding.totalAllWXM.text = formatValue(totalAllTime)
        }

        updateLastReward(tokenInfo?.lastReward)

        if (binding.tokenOptions.checkedChipId == R.id.option7D) {
            setChartData(tokenInfo?.chart7d, tokenInfo?.max7dReward)
        } else {
            setChartData(tokenInfo?.chart30d, tokenInfo?.max30dReward)
        }
    }

    private fun updateLastReward(data: Transaction?) {
        binding.lastReward.text =  data?.actualReward?.let {
            resources.getString(R.string.reward, formatTokens(it))
        } ?: resources.getString(R.string.reward, formatTokens(0F))

        binding.score.text = data?.validationScore?.let {
            resources.getString(R.string.score, it)
        } ?: resources.getString(R.string.score_unknown)

        val color = resHelper.getColor(Tokens.getRewardScoreColor(data?.validationScore))
        binding.scoreIcon.setColorFilter(color)

        with(binding.rewardTimestamp) {
            if (data?.timestamp == null) {
                visibility = View.GONE
            } else {
                val day = getRelativeDayFromISO(resHelper, data.timestamp, true)
                val time = getHourMinutesFromISO(context, data.timestamp)
                val dateAndTime = "$day, $time"
                text = dateAndTime
            }
        }
    }

    private fun setChartData(data: TokenValuesChart?, maxReward: Float?) {
        chart(data?.values)

        if (maxReward != null) {
            if (maxReward > VERY_SMALL_NUMBER_FOR_CHART) {
                binding.maxReward.text =
                    resources.getString(R.string.wxm_amount, formatTokens(maxReward))
            } else {
                binding.maxReward.text = resources.getString(R.string.no_rewards)
            }
        }
    }

    private fun chart(data: List<Pair<String, Float>>?) {
        if (!data.isNullOrEmpty()) {
            //  Necessary fix for a crash, found the fix on library's BarChartView.kt line 85
            binding.tokenChart.barsColorsList =
                List(data.size) { binding.tokenChart.barsColor }.toList()
            binding.tokenChart.show(data)
            binding.tokenChart.visibility = View.VISIBLE
        } else {
            binding.tokenChart.visibility = View.INVISIBLE
        }
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
