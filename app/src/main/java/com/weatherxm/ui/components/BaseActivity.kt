package com.weatherxm.ui.components

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.os.Build.VERSION_CODES.S
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.ui.Navigator
import com.weatherxm.util.AppBuildConfig.versionSDK
import com.weatherxm.util.checkPermissionsAndThen
import com.weatherxm.util.hasPermission
import com.weatherxm.util.permissionsBuilder
import org.koin.android.ext.android.inject

open class BaseActivity : AppCompatActivity(), BaseInterface {
    override val analytics: AnalyticsWrapper by inject()
    override val navigator: Navigator by inject()
    override var snackbar: Snackbar? = null

    /**
     * Suppress NewApi because we use versionSDK to get the current API level
     */
    @SuppressLint("NewApi")
    protected fun requestToEnableBluetooth(onGranted: () -> Unit, onDenied: () -> Unit) {
        if (versionSDK() >= S) {
            checkPermissionsAndThen(
                permissions = arrayOf(BLUETOOTH_CONNECT),
                rationaleTitle = getString(R.string.permission_bluetooth_title),
                rationaleMessage = getString(R.string.perm_bluetooth_scanning_desc),
                onGranted = onGranted,
                onDenied = onDenied
            )
        } else {
            onGranted()
        }
    }

    /**
     * Suppress NewApi because we use versionSDK to get the current API level
     */
    @SuppressLint("NewApi")
    protected fun requestBluetoothPermissions(onGranted: () -> Unit, onDenied: () -> Unit) {
        if (versionSDK() >= S) {
            checkPermissionsAndThen(
                permissions = arrayOf(BLUETOOTH_SCAN, BLUETOOTH_CONNECT),
                rationaleTitle = getString(R.string.permission_bluetooth_title),
                rationaleMessage = getString(R.string.perm_bluetooth_scanning_desc),
                onGranted = onGranted,
                onDenied = onDenied
            )
        } else {
            checkPermissionsAndThen(
                permissions = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                rationaleTitle = getString(R.string.permission_location_title),
                rationaleMessage = getString(R.string.perm_location_scanning_desc),
                onGranted = onGranted,
                onDenied = onDenied
            )
        }
    }

    /**
     * Suppress NewApi because we use versionSDK to get the current API level
     */
    @SuppressLint("NewApi")
    protected fun requestNotificationsPermissions() {
        if (!hasPermission(POST_NOTIFICATIONS) && versionSDK() >= TIRAMISU) {
            permissionsBuilder(permissions = arrayOf(POST_NOTIFICATIONS)).build().send()
        }
    }
}
