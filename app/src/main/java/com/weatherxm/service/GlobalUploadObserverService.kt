package com.weatherxm.service

import android.content.Context
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UploadPhotosState
import com.weatherxm.ui.common.empty
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

class GlobalUploadObserverService : RequestObserverDelegate {

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
                false
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
        onUploadPhotosState.tryEmit(
            UploadPhotosState(
                device,
                uploadInfo.progressPercent,
                true
            )
        )
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
                false,
                uploadInfo
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
