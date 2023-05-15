package com.weatherxm.ui.startup

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.weatherxm.ui.Navigator
import com.weatherxm.util.Analytics
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StartupActivity : AppCompatActivity(), KoinComponent {
    private val model: StartupViewModel by viewModels()
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { true }

        model.startup().observe(this) { state ->
            when (state) {
                StartupState.ShowExplorer -> navigator.showExplorer(this)
                StartupState.ShowHome -> navigator.showHome(this)
                StartupState.ShowAnalyticsOptIn -> navigator.showAnalyticsOptIn(this)
                StartupState.ShowUpdate -> navigator.showUpdatePrompt(this)
            }
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.SPLASH,
            StartupActivity::class.simpleName
        )
    }
}
