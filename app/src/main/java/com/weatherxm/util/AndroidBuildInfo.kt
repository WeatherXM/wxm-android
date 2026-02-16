package com.weatherxm.util

import android.os.Build
import com.weatherxm.BuildConfig

object AndroidBuildInfo {
    val sdkInt: Int = Build.VERSION.SDK_INT
    val release: String? = Build.VERSION.RELEASE
    val manufacturer: String? = Build.MANUFACTURER
    val model: String? = Build.MODEL
    val isSolana: Boolean = BuildConfig.FLAVOR_server == "solana"
}
