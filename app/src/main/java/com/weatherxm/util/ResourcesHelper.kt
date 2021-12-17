package com.weatherxm.util

import android.content.res.Resources
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import org.koin.core.component.KoinComponent

class ResourcesHelper(private val resources: Resources) : KoinComponent {

    @ColorInt
    fun getColor(@ColorRes colorResId: Int): Int {
        return ResourcesCompat.getColor(resources, colorResId, null)
    }

    fun getString(@StringRes stringRes: Int): String {
        return resources.getString(stringRes)
    }
}
