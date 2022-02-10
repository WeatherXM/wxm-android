package com.weatherxm.app

import android.app.Application
import com.weatherxm.BuildConfig
import com.weatherxm.data.modules
import com.weatherxm.util.CrashReportingTree
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Setup DI
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@App)
            modules(modules)
        }

        // Setup debug logs
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Setup crash reporting
        Timber.plant(CrashReportingTree())
    }
}
