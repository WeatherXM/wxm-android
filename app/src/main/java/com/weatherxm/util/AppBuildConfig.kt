package com.weatherxm.util

import android.os.Build

object AppBuildConfig {
    fun versionSDK(): Int {
        return Build.VERSION.SDK_INT
    }
}
