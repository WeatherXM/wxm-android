package com.weatherxm.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class CrashReportingTree : Timber.Tree(), KoinComponent {
    private val firebaseCrashlytics: FirebaseCrashlytics by inject()

    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        // Only log on WARN and ERROR
        if (priority != Log.WARN || priority != Log.ERROR) {
            return
        }

        // Log message
        firebaseCrashlytics.log(message)

        // Log exception
        throwable?.let {
            firebaseCrashlytics.recordException(it)
        }
    }
}
