package com.weatherxm.util

import android.os.Build
import com.weatherxm.BuildConfig

object AndroidBuildInfo {
    val sdkInt: Int = Build.VERSION.SDK_INT
    val release: String? = Build.VERSION.RELEASE
    val manufacturer: String? = Build.MANUFACTURER
    val model: String? = Build.MODEL

    fun isSolana(): Boolean {
        /**
         * False warning by Android Studio on the APPLICATION_ID check when in non-Solana flavors.
         * It will be true when the Solana flavor is used.
         */
        return manufacturer?.contains("Solana", true) == true ||
            BuildConfig.APPLICATION_ID == "com.weatherxm.app.solana"
    }
}
