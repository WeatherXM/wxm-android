package com.weatherxm.ui.splash

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.ui.Navigator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class SplashActivity : AppCompatActivity(), KoinComponent {

    private val model: SplashViewModel by viewModels()
    private val navigator: Navigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model.getFirebaseRemoteConfig().fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    model.shouldPromptUpdate()
                } else {
                    Timber.w(task.exception, "Firebase Fetch And Activate Failed")
                    model.checkIfLoggedIn()
                }
            }

        model.shouldUpdate().observe(this) {
            if (it) {
                navigator.showUpdatePrompt(this)
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }

        model.isLoggedIn().observe(this) { result ->
            result.mapLeft {
                Timber.d("Not logged in. Show explorer.")
                navigator.showExplorer(this)
            }.map { username ->
                Timber.d("Already logged in as $username")
                navigator.showHome(this)
            }
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
