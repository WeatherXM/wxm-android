package com.weatherxm.ui.components

import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.ui.Navigator
import com.weatherxm.analytics.AnalyticsImpl
import org.koin.android.ext.android.inject

open class BaseFragment : Fragment(), BaseInterface {
    override val analytics: AnalyticsImpl by inject()
    override val navigator: Navigator by inject()
    override var snackbar: Snackbar? = null
}
