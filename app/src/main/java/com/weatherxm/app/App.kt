package com.weatherxm.app

import android.app.Application
import com.google.firebase.messaging.FirebaseMessaging
import com.weatherxm.BuildConfig
import com.weatherxm.data.modules
import com.weatherxm.util.CrashReportingTree
import com.weatherxm.util.DisplayModeHelper
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class App : Application() {

    private val firebaseMessaging: FirebaseMessaging by inject()
    private val displayModeHelper: DisplayModeHelper by inject()

    override fun onCreate() {
        super.onCreate()

        // Setup DI
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@App)
            modules(modules)
        }

        // Setup debug logs or crash reporting depending on the build type
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }

        // Set light/dark theme
        displayModeHelper.setDisplayMode()

        // Print debug info
        printDebugInfo()
    }

    private fun printDebugInfo() {
        if (BuildConfig.DEBUG) {
            // Log Firebase Cloud Messaging token for testing
            firebaseMessaging.token
                .addOnSuccessListener { token ->
                    Timber.d("FCM registration token: $token")
                }
                .addOnFailureListener {
                    Timber.w(it, "Could not get FCM token.")
                }
        }
    }
}
