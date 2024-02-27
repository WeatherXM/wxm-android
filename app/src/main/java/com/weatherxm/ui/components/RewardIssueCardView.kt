package com.weatherxm.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.google.android.material.button.MaterialButton.ICON_GRAVITY_END
import com.weatherxm.databinding.ViewRewardIssueBinding
import com.weatherxm.ui.common.setCardStroke

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
            visibility = if (title != null) View.VISIBLE else View.GONE
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
            visibility = if (subtitle != null) View.VISIBLE else View.GONE
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
        endIcon: Drawable? = null,
        listener: OnClickListener
    ): RewardIssueCardView {
        with(binding.action) {
            text = label
            setOnClickListener(listener)
            visibility = VISIBLE
            if (endIcon != null) {
                icon = endIcon
                iconGravity = ICON_GRAVITY_END
            }
        }
        return this
    }
}
