package com.weatherxm.ui.analytics

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.databinding.ActivityAnalyticsOptInBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.util.Analytics
import com.weatherxm.util.applyInsets
import com.weatherxm.util.setHtml
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AnalyticsOptInActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityAnalyticsOptInBinding
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()
    private val model: AnalyticsOptInViewModel by viewModels()

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
        analytics.trackScreen(
            Analytics.Screen.ANALYTICS,
            AnalyticsOptInActivity::class.simpleName
        )
    }
}
