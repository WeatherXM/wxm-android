package com.weatherxm.app

import android.content.Context
import androidx.startup.Initializer
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.weatherxm.BuildConfig
import com.weatherxm.data.services.CacheService
import com.weatherxm.util.CrashReportingTree
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class CrashlyticsInitializer : Initializer<Unit>, KoinComponent {
    private val cacheService: CacheService by inject()

    override fun create(context: Context) {
        Timber.d("Basic configuration for Firebase Crashlytics")
        Firebase.crashlytics.also {
            // Setup crash reporting on RELEASE builds
            if (!BuildConfig.DEBUG) {
                Timber.plant(CrashReportingTree(it))
                Timber.d("Enabled Crashlytics crash reporting")
            } else {
                Timber.d("Crashlytics crash reporting disabled in DEBUG builds")
            }
            it.setUserId(cacheService.getUserId())
            cacheService.getInstallationId().onRight { installationId ->
                it.setCustomKey(CacheService.KEY_INSTALLATION_ID, installationId)
            }
        }
        return
    }

    override fun dependencies(): List<Class<AppInitializer>> {
        return listOf(AppInitializer::class.java)
    }
}
