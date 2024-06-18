package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.weatherxm.databinding.ViewRewardIssueBinding
import com.weatherxm.ui.common.setCardStroke
import com.weatherxm.ui.common.setVisible

class RewardIssueCardView : LinearLayout {

    private lateinit var binding: ViewRewardIssueBinding

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
        binding = ViewRewardIssueBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    fun title(@StringRes resId: Int): RewardIssueCardView {
        title(resources.getString(resId))
        return this
    }

    fun title(title: String?): RewardIssueCardView {
        binding.title.apply {
            text = title
            setVisible(title != null)
        }
        return this
    }

    fun message(@StringRes resId: Int): RewardIssueCardView {
        message(resources.getString(resId))
        return this
    }

    fun message(subtitle: String?): RewardIssueCardView {
        binding.message.apply {
            text = subtitle
            setVisible(subtitle != null)
        }
        return this
    }

    fun setBackground(@ColorRes colorResId: Int): RewardIssueCardView {
        binding.card.setCardBackgroundColor(context.getColor(colorResId))
        return this
    }

    fun setStrokeColor(@ColorRes colorResId: Int): RewardIssueCardView {
        binding.card.setCardStroke(colorResId, 2)
        return this
    }

    fun action(
        label: String,
        listener: OnClickListener
    ): RewardIssueCardView {
        with(binding.action) {
            text = label
            setOnClickListener(listener)
            setVisible(true)
        }
        return this
    }
}
