package com.weatherxm.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * [Timber.Tree] that logs non-fatal exceptions to FirebaseCrashlytics.
 * Only logs for WARN and ERROR log levels. Ignores all else.
 */
class CrashReportingTree(
    private val crashlytics: FirebaseCrashlytics
) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Only log on WARN and ERROR
        if (priority == Log.WARN || priority == Log.ERROR) {
            // Log message
            crashlytics.log(message)

            // Log exception
            t?.let {
                crashlytics.recordException(it)
            }
        }
    }
}
