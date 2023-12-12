package com.weatherxm.ui.home.profile

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.databinding.ViewProfileItemCardBinding
import com.weatherxm.ui.common.hide
import com.weatherxm.ui.common.setVisible

class ProfileItemCardView : LinearLayout {

    private lateinit var binding: ViewProfileItemCardBinding

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
        binding = ViewProfileItemCardBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER

        val attributes =
            this.context.theme.obtainStyledAttributes(attrs, R.styleable.ProfileItemCardView, 0, 0)

        try {
            attributes.getString(R.styleable.ProfileItemCardView_profile_item_title)?.let {
                title(it)
            } ?: binding.title.hide(null)

            val subtitleText =
                attributes.getString(R.styleable.ProfileItemCardView_profile_item_subtitle)
            if (!subtitleText.isNullOrEmpty()) {
                subtitle(subtitleText)
            }

            attributes.getResourceId(R.styleable.ProfileItemCardView_profile_item_icon, 0).apply {
                if (this != 0) {
                    binding.icon.setImageResource(this)
                }
                binding.icon.setVisible(this != 0)
            }
        } finally {
            attributes.recycle()
        }
    }

    fun title(subtitle: String?): ProfileItemCardView {
        binding.title.apply {
            text = subtitle
            visibility = if (subtitle != null) View.VISIBLE else View.GONE
        }
        return this
    }

    fun subtitle(subtitle: String?): ProfileItemCardView {
        binding.subtitle.apply {
            text = subtitle
            visibility = if (subtitle != null) View.VISIBLE else View.GONE
        }
        return this
    }

    fun chipSubtitle(subtitle: String?): ProfileItemCardView {
        binding.chipSubtitle.apply {
            text = subtitle
            visibility = if (subtitle != null) View.VISIBLE else View.GONE
        }
        return this
    }

    fun action(
        label: String,
        listener: OnClickListener
    ): ProfileItemCardView {
        with(binding.action) {
            text = label
            setOnClickListener(listener)
            visibility = VISIBLE
        }
        return this
    }

    fun clear() {
        binding.subtitle.setVisible(false)
        binding.chipSubtitle.setVisible(false)
        binding.action.setVisible(false)
    }
}
