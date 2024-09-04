package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.databinding.ViewEmptyRewardsCardBinding
import com.weatherxm.ui.common.visible

class EmptyRewardsCardView : LinearLayout {

    private lateinit var binding: ViewEmptyRewardsCardBinding

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
        init(context, attrs)
    }

    private fun init(context: Context?, attrs: AttributeSet? = null) {
        binding = ViewEmptyRewardsCardBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.EmptyRewardsCardView, 0, 0)
            .apply {
                try {
                    binding.proTipCard.visible(
                        getBoolean(R.styleable.EmptyRewardsCardView_show_pro_tip, true)
                    )
                } finally {
                    recycle()
                }
            }
    }
}
