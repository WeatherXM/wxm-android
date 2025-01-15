package com.weatherxm.service

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UploadPhotosState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.exceptions.UploadError
import net.gotev.uploadservice.exceptions.UserCancelledUploadException
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import timber.log.Timber
import java.io.File

class GlobalUploadObserverService(
    private val analytics: AnalyticsWrapper,
    private val cacheService: CacheService
) : RequestObserverDelegate {

    private var device = UIDevice.empty()
    private val onUploadPhotosState = MutableSharedFlow<UploadPhotosState>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun onProgress(context: Context, uploadInfo: UploadInfo) {
        Timber.d("[UPLOAD SERVICE] Progress: ${uploadInfo.progressPercent}")
        onUploadPhotosState.tryEmit(
            UploadPhotosState(
                device,
                uploadInfo.progressPercent,
                isSuccess = false,
                isError = false
            )
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onSuccess(
        context: Context,
        uploadInfo: UploadInfo,
        serverResponse: ServerResponse
    ) {
        Timber.d("[UPLOAD SERVICE] Success: $serverResponse")
        analytics.trackEventViewContent(
            contentName = AnalyticsService.ParamValue.UPLOADING_PHOTOS_SUCCESS.paramValue,
            null,
            Pair(FirebaseAnalytics.Param.ITEM_ID, device.name)
        )
        onUploadPhotosState.tryEmit(
            UploadPhotosState(
                device,
                uploadInfo.progressPercent,
                isSuccess = true,
                isError = false
            )
        )
        uploadInfo.files.forEach {
            File(it.path).delete()
        }
        cacheService.removeUploadIdRequest(uploadInfo.uploadId)
        cacheService.removeDevicePhotoUploadId(device.id, uploadInfo.uploadId)
        onUploadPhotosState.resetReplayCache()
    }

    override fun onError(context: Context, uploadInfo: UploadInfo, exception: Throwable) {
        when (exception) {
            is UserCancelledUploadException -> {
                Timber.e(exception, "[UPLOAD SERVICE] User Cancelled: $uploadInfo")
            }
            is UploadError -> {
                Timber.e(exception, "[UPLOAD SERVICE] Error: ${exception.serverResponse}")
            }
            else -> {
                Timber.e(exception, "[UPLOAD SERVICE] Error: $uploadInfo")
            }
        }
        onUploadPhotosState.tryEmit(
            UploadPhotosState(
                device,
                uploadInfo.progressPercent,
                isSuccess = false,
                isError = true
            )
        )
    }

    override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
        Timber.d("[UPLOAD SERVICE] Completed: $uploadInfo")
    }

    override fun onCompletedWhileNotObserving() {
        Timber.d("[UPLOAD SERVICE] Completed while not observing")
    }

    fun setDevice(device: UIDevice) {
        this.device = device
    }

    fun getUploadPhotosState(): Flow<UploadPhotosState> {
        return onUploadPhotosState
    }
}
