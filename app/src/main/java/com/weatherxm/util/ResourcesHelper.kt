package com.weatherxm.util

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.weatherxm.R

class ResourcesHelper(private val resources: Resources) {

    @ColorInt
    fun getColor(@ColorRes colorResId: Int): Int {
        return ResourcesCompat.getColor(resources, colorResId, null)
    }

    fun getString(@StringRes stringRes: Int): String {
        return resources.getString(stringRes)
    }

    fun getString(@StringRes stringRes: Int, concatenatedString: String): String {
        return resources.getString(stringRes, concatenatedString)
    }

    fun getWindDirectionDrawable(index: Int): Drawable {
        val windDirectionDrawables: LayerDrawable = ResourcesCompat.getDrawable(
            resources,
            R.drawable.layers_wind_direction,
            null
        ) as LayerDrawable
        return windDirectionDrawables.getDrawable(index)
    }
}
