package com.weatherxm.ui.analytics

import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.analytics.Analytics
import com.weatherxm.databinding.ActivityAnalyticsOptInBinding
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.common.getClassSimpleName
import org.koin.androidx.viewmodel.ext.android.viewModel

class AnalyticsOptInActivity : BaseActivity() {
    private lateinit var binding: ActivityAnalyticsOptInBinding
    private val model: AnalyticsOptInViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsOptInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.message.setHtml(R.string.google_analytics_explanation_message)

        binding.deny.setOnClickListener {
            model.setAnalyticsEnabled(false)
            navigator.showHome(this)
            finish()
        }

        binding.ok.setOnClickListener {
            model.setAnalyticsEnabled(true)
            navigator.showHome(this)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Analytics.Screen.ANALYTICS, getClassSimpleName())
    }
}
