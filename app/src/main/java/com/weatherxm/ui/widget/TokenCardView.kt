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
import com.weatherxm.ui.common.TokenInfo
import com.weatherxm.ui.common.show
import com.weatherxm.util.DateTimeHelper.getFormattedDay
import com.weatherxm.util.DateTimeHelper.getFormattedTime
import com.weatherxm.util.Tokens
import com.weatherxm.util.Tokens.formatTokens
import com.weatherxm.util.Tokens.formatValue
import com.weatherxm.util.initializeTokenChart
import com.weatherxm.util.setChildrenEnabled

open class TokenCardView : LinearLayout {

    private lateinit var binding: ViewTokenCardBinding

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
                    binding.chart.clear()
                    tokenInfo?.chart7dEntries?.let {
                        binding.chart.initializeTokenChart(it)
                    }
                    setMaxReward(tokenInfo?.max7dReward)
                }
                R.id.option30D -> {
                    binding.chart.clear()
                    tokenInfo?.chart30dEntries?.let {
                        binding.chart.initializeTokenChart(it)
                    }
                    setMaxReward(tokenInfo?.max30dReward)
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
            tokenInfo?.chart7dEntries?.let {
                binding.chart.initializeTokenChart(it)
                binding.chart.show(null)
            }
            setMaxReward(tokenInfo?.max7dReward)
        } else {
            tokenInfo?.chart30dEntries?.let {
                binding.chart.initializeTokenChart(it)
                binding.chart.show(null)
            }
            setMaxReward(tokenInfo?.max30dReward)
        }
    }

    private fun updateLastReward(data: Transaction?) {
        binding.lastReward.text = data?.actualReward?.let {
            resources.getString(R.string.reward, formatTokens(it))
        } ?: resources.getString(R.string.reward, formatTokens(0F))

        binding.score.text = data?.validationScore?.let {
            resources.getString(R.string.score, it)
        } ?: resources.getString(R.string.score_unknown)

        val color =
            resources.getColor(Tokens.getRewardScoreColor(data?.validationScore), context.theme)
        binding.scoreIcon.setColorFilter(color)

        with(binding.rewardTimestamp) {
            if (data?.timestamp == null) {
                visibility = View.GONE
            } else {
                val day = data.timestamp.getFormattedDay(context, true)
                val time = data.timestamp.getFormattedTime(context)
                text = listOf(day, time).joinToString(separator = ", ")
            }
        }
    }

    private fun setMaxReward(maxReward: Float?) {
        if (maxReward != null) {
            if (maxReward > VERY_SMALL_NUMBER_FOR_CHART) {
                binding.maxReward.text =
                    resources.getString(R.string.wxm_amount, formatTokens(maxReward))
            } else {
                binding.maxReward.text = resources.getString(R.string.no_rewards)
            }
        }
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
