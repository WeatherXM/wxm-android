package com.weatherxm.ui.components

import android.Manifest
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.toast
import com.weatherxm.util.checkPermissionsAndThen
import timber.log.Timber

interface BaseInterface {
    val analytics: AnalyticsWrapper
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

        try {
            if (callback != null) {
                snackbar = Snackbar.make(viewGroup, message, Snackbar.LENGTH_INDEFINITE)
                snackbar?.setAction(R.string.action_retry) {
                    callback()
                }
            } else {
                snackbar = Snackbar.make(viewGroup, message, Snackbar.LENGTH_LONG)
            }
            snackbar?.show()
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Failed to make Snackbar. ViewGroup - Message: $viewGroup $message")
        }
    }

    fun requestLocationPermissions(activity: FragmentActivity?, onGranted: () -> Unit) {
        activity?.checkPermissionsAndThen(
            permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            rationaleTitle = activity.getString(R.string.permission_location_title),
            rationaleMessage = activity.getString(R.string.permission_location_rationale),
            onGranted = { onGranted.invoke() },
            onDenied = { activity.toast(R.string.error_claim_gps_failed) }
        )
    }
}
