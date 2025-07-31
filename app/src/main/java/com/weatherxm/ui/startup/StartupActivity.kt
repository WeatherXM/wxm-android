package com.weatherxm.ui.startup

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mapbox.common.MapboxOptions
import com.weatherxm.R
import com.weatherxm.ui.components.BaseActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class StartupActivity : BaseActivity() {
    private val model: StartupViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { true }

        // Set access token for Mapbox on startup
        MapboxOptions.accessToken = getString(R.string.mapbox_access_token)

        model.onStartupState().observe(this) { state ->
            when (state) {
                StartupState.ShowHome -> navigator.showHome(this)
                StartupState.ShowAnalyticsOptIn -> navigator.showAnalyticsOptIn(this)
                StartupState.ShowUpdate -> navigator.showUpdatePrompt(this)
                is StartupState.ShowDeepLinkRouter -> {
                    navigator.showDeepLinkRouter(this, state.remoteMessage)
                }
            }
            finish()
        }

        model.handleStartup(intent)
    }
}
