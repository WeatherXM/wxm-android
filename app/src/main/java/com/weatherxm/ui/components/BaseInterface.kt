package com.weatherxm.ui.components

import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.ui.Navigator
import com.weatherxm.util.Analytics

interface BaseInterface {
    val analytics: Analytics
    val navigator: Navigator
    var snackbar: Snackbar?

    fun showSnackbarMessage(
        viewGroup: ViewGroup,
        message: String,
        callback: (() -> Unit)? = null
    ) {
        if (snackbar?.isShown == true) {
            snackbar?.dismiss()
        }

        if (callback != null) {
            snackbar = Snackbar.make(viewGroup, message, Snackbar.LENGTH_INDEFINITE)
            snackbar?.setAction(R.string.action_retry) {
                callback()
            }
        } else {
            snackbar = Snackbar.make(viewGroup, message, Snackbar.LENGTH_LONG)
        }
        snackbar?.show()
    }
}
