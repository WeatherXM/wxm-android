package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.databinding.ViewDeviceTypeBinding
import org.koin.core.component.KoinComponent

open class DeviceTypeCardView : LinearLayout, KoinComponent {

    private lateinit var binding: ViewDeviceTypeBinding

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
        binding = ViewDeviceTypeBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL

        val attributes =
            this.context.theme.obtainStyledAttributes(attrs, R.styleable.DeviceTypeCardView, 0, 0)

        attributes.getString(R.styleable.DeviceTypeCardView_device_type_title)?.let {
            binding.title.text = it
        }

        attributes.getResourceId(R.styleable.DeviceTypeCardView_device_type_image, 0).apply {
            if (this != 0) {
                binding.image.setImageResource(this)
            }
        }
        attributes.recycle()
    }

    fun listener(onClicked: () -> Unit) {
        binding.card.setOnClickListener {
            onClicked.invoke()
        }
    }
}
