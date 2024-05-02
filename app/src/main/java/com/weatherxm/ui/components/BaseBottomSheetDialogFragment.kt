package com.weatherxm.ui.components

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import org.koin.android.ext.android.inject

open class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {
    protected val analytics: AnalyticsWrapper by inject()

    override fun getTheme(): Int {
        return R.style.ThemeOverlay_WeatherXM_BottomSheetDialog
    }
}
