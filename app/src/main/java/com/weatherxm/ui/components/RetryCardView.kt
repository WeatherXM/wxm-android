package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.weatherxm.databinding.ViewRetryCardBinding

class RetryCardView : ConstraintLayout {

    private lateinit var binding: ViewRetryCardBinding

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
        binding = ViewRetryCardBinding.inflate(LayoutInflater.from(context), this)
    }


    fun listener(listener: () -> Unit) = binding.retryCard.setOnClickListener { listener.invoke() }
}
