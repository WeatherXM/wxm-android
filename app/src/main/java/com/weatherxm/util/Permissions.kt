package com.weatherxm.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import com.fondesa.kpermissions.allDenied
import com.fondesa.kpermissions.allPermanentlyDenied
import com.fondesa.kpermissions.anyGranted
import com.fondesa.kpermissions.anyShouldShowRationale
import com.fondesa.kpermissions.builder.PermissionRequestBuilder
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.extension.send
import com.weatherxm.R
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.ActionDialogFragment
import timber.log.Timber

@Suppress("LongParameterList")
fun FragmentActivity.checkPermissionsAndThen(
    vararg permissions: String,
    rationaleTitle: String,
    rationaleMessage: String,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    showOnPermanentlyDenied: Boolean = true
) {
    // Show rationale dialog
    fun onShowRationale() {
        ActionDialogFragment.Builder()
            .title(rationaleTitle)
            .message(rationaleMessage)
            .onPositiveClick(getString(R.string.action_ok)) {
                Timber.d("Recheck again")
                checkPermissionsAndThen(
                    permissions = permissions,
                    rationaleTitle = rationaleTitle,
                    rationaleMessage = rationaleMessage,
                    onGranted = onGranted,
                    onDenied = onDenied,
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
        ActionDialogFragment.Builder()
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
        if (result.allDenied()) {
            onDenied()
        }

        when {
            result.anyGranted() -> onGranted()
            result.anyShouldShowRationale() -> onShowRationale()
            result.allPermanentlyDenied() -> {
                if (showOnPermanentlyDenied) {
                    onPermanentlyDenied()
                }
            }
        }
    }
}

fun Context.hasPermission(permission: String): Boolean {
    return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}

fun FragmentActivity.permissionsBuilder(
    vararg permissions: String
): PermissionRequestBuilder {
    require(permissions.isNotEmpty()) { "You need to define at least one permission." }
    return permissionsBuilder(
        firstPermission = permissions[0],
        otherPermissions = permissions.drop(1).toTypedArray()
    )
}
