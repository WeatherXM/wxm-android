package com.weatherxm.ui.common

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.fondesa.kpermissions.allGranted
import com.fondesa.kpermissions.anyPermanentlyDenied
import com.fondesa.kpermissions.anyShouldShowRationale
import com.fondesa.kpermissions.builder.PermissionRequestBuilder
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.extension.send
import com.weatherxm.R
import timber.log.Timber

fun FragmentActivity.hasPermission(
    permission: String
): Boolean = ActivityCompat.checkSelfPermission(
    this,
    permission
) == PackageManager.PERMISSION_GRANTED

fun FragmentActivity.hasPermissions(
    vararg permissions: String
): Boolean = permissions.all { permission ->
    hasPermission(permission)
}

fun Fragment.checkPermissionsAndThen(
    vararg permissions: String,
    rationaleTitle: String,
    rationaleMessage: String,
    onGranted: () -> Unit,
    onDenied: () -> Unit
) {
    activity?.checkPermissionsAndThen(
        permissions = permissions,
        rationaleTitle = rationaleTitle,
        rationaleMessage = rationaleMessage,
        onGranted = onGranted,
        onDenied = onDenied
    )
}

fun FragmentActivity.checkPermissionsAndThen(
    vararg permissions: String,
    rationaleTitle: String,
    rationaleMessage: String,
    onGranted: () -> Unit,
    onDenied: () -> Unit
) {
    // Show rationale dialog
    fun onShowRationale() {
        AlertDialogFragment.Builder()
            .title(rationaleTitle)
            .message(rationaleMessage)
            .onPositiveClick(getString(R.string.action_ok)) {
                Timber.d("Recheck again")
                checkPermissionsAndThen(
                    permissions = permissions,
                    rationaleTitle = rationaleTitle,
                    rationaleMessage = rationaleMessage,
                    onGranted = onGranted,
                    onDenied = onDenied
                )
            }
            .onNegativeClick(getString(R.string.action_cancel)) {
                Timber.d("Cancelled")
                toast(R.string.warn_cancelled)
            }
            .build()
            .show(this)
    }

    // Show application settings prompt
    fun onPermanentlyDenied() {
        AlertDialogFragment.Builder()
            .title(getString(R.string.permission_permanently_denied_title))
            .message(getString(R.string.permission_permanently_denied_rationale))
            .onPositiveClick(getString(R.string.action_ok)) {
                Timber.d("Going to application settings")
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .onNegativeClick(getString(R.string.action_cancel)) {
                Timber.d("Cancelled prompt")
                toast(R.string.warn_cancelled)
            }
            .build()
            .show(this)
    }

    permissionsBuilder(
        permissions = permissions
    ).build().send { result ->
        when {
            result.allGranted() -> onGranted()
            result.anyShouldShowRationale() -> onShowRationale()
            result.anyPermanentlyDenied() -> {
                onPermanentlyDenied()
                onDenied()
            }
        }
    }
}

private fun FragmentActivity.permissionsBuilder(
    vararg permissions: String
): PermissionRequestBuilder {
    if (permissions.isEmpty()) {
        throw IllegalArgumentException("You need to define at least one permission.")
    }
    return permissionsBuilder(
        firstPermission = permissions[0],
        otherPermissions = permissions.drop(1).toTypedArray()
    )
}
