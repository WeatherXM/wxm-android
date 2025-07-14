package com.weatherxm.ui.components

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Build.VERSION_CODES.S
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.PhotoPresignedMetadata
import com.weatherxm.service.workers.UploadPhotoWorker
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.util.AndroidBuildInfo
import com.weatherxm.util.ImageFileHelper.compressImageFile
import com.weatherxm.util.ImageFileHelper.copyExifMetadata
import com.weatherxm.util.ImageFileHelper.copyInputStreamToFile
import com.weatherxm.util.checkPermissionsAndThen
import com.weatherxm.util.hasPermission
import com.weatherxm.util.permissionsBuilder
import org.koin.android.ext.android.inject
import java.io.File

open class BaseActivity : AppCompatActivity(), BaseInterface {
    override val analytics: AnalyticsWrapper by inject()
    override val navigator: Navigator by inject()
    override var snackbar: Snackbar? = null

    /**
     * Suppress InlinedApi as we check for API level before using it through AndroidBuildInfo.sdkInt
     */
    @SuppressLint("InlinedApi")
    protected fun requestToEnableBluetooth(onGranted: () -> Unit, onDenied: () -> Unit) {
        if (AndroidBuildInfo.sdkInt >= S) {
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
     * Suppress InlinedApi as we check for API level before using it through AndroidBuildInfo.sdkInt
     */
    @SuppressLint("InlinedApi")
    protected fun requestBluetoothPermissions(onGranted: () -> Unit, onDenied: () -> Unit) {
        if (AndroidBuildInfo.sdkInt >= S) {
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
     * Suppress InlinedApi as we check for API level before using it through AndroidBuildInfo.sdkInt
     */
    @SuppressLint("InlinedApi")
    protected fun requestNotificationsPermissions() {
        if (!hasPermission(POST_NOTIFICATIONS) && AndroidBuildInfo.sdkInt >= TIRAMISU) {
            permissionsBuilder(permissions = arrayOf(POST_NOTIFICATIONS)).build().send()
        }
    }

    protected fun openLearnMoreDialog(
        @StringRes titleResId: Int?,
        @StringRes messageResId: Int,
        messageSource: String
    ) {
        navigator.showMessageDialog(
            supportFragmentManager,
            title = titleResId?.let { getString(it) },
            message = getString(messageResId)
        )
        analytics.trackEventSelectContent(
            AnalyticsService.ParamValue.LEARN_MORE.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, messageSource)
        )
    }

    fun startWorkerForUploadingPhotos(
        device: UIDevice,
        photos: List<StationPhoto>,
        photosPresignedMetadata: List<PhotoPresignedMetadata>,
        defaultPhotoPrefix: String,
    ) {
        photos.forEachIndexed { index, stationPhoto ->
            photosPresignedMetadata.getOrNull(index)?.let { metadata ->
                /**
                 * Use the current photo name in cache otherwise default to "deviceId_img$index.jpg"
                 */
                val fileName = stationPhoto.localPath?.substringAfterLast('/')
                    ?: "${defaultPhotoPrefix}_img$index.jpg"
                val file = File(cacheDir, fileName)
                val imageBitmap = BitmapFactory.decodeFile(stationPhoto.localPath)
                file.copyInputStreamToFile(compressImageFile(imageBitmap))
                copyExifMetadata(
                    stationPhoto.localPath,
                    file.path,
                    stationPhoto.source?.exifUserComment
                )

                // Start the work manager to upload the photo.
                UploadPhotoWorker.initAndStart(this, metadata, file.path, device.id)
            }
        }
    }
}
