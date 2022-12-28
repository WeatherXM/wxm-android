package com.weatherxm.ui.common

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Parcelable

inline fun <reified T : Parcelable?> Activity.getParcelableExtra(key: String, fallback: T): T {
    val value: T? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(key, T::class.java)
    } else {
        intent.getParcelableExtra<T>(key)
    }
    return value ?: fallback
}

inline fun <reified T : Parcelable?> Intent.getParcelableExtra(key: String, fallback: T): T {
    val value: T? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        getParcelableExtra<T>(key)
    }
    return value ?: fallback
}
