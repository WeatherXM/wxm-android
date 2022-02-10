package com.weatherxm.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        // Only log on WARN and ERROR
        if (priority != Log.WARN || priority != Log.ERROR) {
            return
        }

        // Log message
        FirebaseCrashlytics.getInstance().log(message)

        // Log exception
        throwable?.let {
            FirebaseCrashlytics.getInstance().recordException(it)
        }
    }
}
