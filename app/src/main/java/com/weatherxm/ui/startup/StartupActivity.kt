package com.weatherxm.ui.startup

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.weatherxm.ui.Navigator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StartupActivity : AppCompatActivity(), KoinComponent {
    private val model: StartupViewModel by viewModels()
    private val navigator: Navigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { true }

        model.startup().observe(this) { state ->
            when (state) {
                StartupState.ShowExplorer -> navigator.showExplorer(this)
                StartupState.ShowHome -> navigator.showHome(this)
                StartupState.ShowUpdate -> navigator.showUpdatePrompt(this)
            }
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
