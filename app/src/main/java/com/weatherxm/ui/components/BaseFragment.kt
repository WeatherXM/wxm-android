package com.weatherxm.ui.components

import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.ui.Navigator
import com.weatherxm.analytics.AnalyticsWrapper
import org.koin.android.ext.android.inject

open class BaseFragment : Fragment(), BaseInterface {
    override val analytics: AnalyticsWrapper by inject()
    override val navigator: Navigator by inject()
    override var snackbar: Snackbar? = null
}
