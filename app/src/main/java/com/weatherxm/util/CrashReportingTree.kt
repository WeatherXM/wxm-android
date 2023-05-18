package com.weatherxm.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * [Timber.Tree] that logs non-fatal exceptions to FirebaseCrashlytics.
 * Only logs for WARN and ERROR log levels. Ignores all else.
 */
class CrashReportingTree : Timber.Tree(), KoinComponent {
    private val firebaseCrashlytics: FirebaseCrashlytics by inject()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Only log on WARN and ERROR
        if (priority == Log.WARN || priority == Log.ERROR) {
            // Log message
            firebaseCrashlytics.log(message)

            // Log exception
            t?.let {
                firebaseCrashlytics.recordException(it)
            }
        }
    }
}
