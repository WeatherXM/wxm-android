package com.weatherxm.util

import android.content.res.Resources
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes

class Resources(private val resources: Resources) {

    @ColorInt
    fun getColor(@ColorRes colorResId: Int): Int {
        // TODO: How to automatically return colors-night or colors?
        return resources.getColor(colorResId, null)
    }

    fun getString(@StringRes stringRes: Int): String {
        return resources.getString(stringRes)
    }

    fun getString(@StringRes stringRes: Int, concatenatedString: String): String {
        return resources.getString(stringRes, concatenatedString)
    }
}
