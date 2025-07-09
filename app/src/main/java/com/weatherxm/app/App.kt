package com.weatherxm.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.weatherxm.BuildConfig
import com.weatherxm.service.GlobalUploadObserverService
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.observer.request.GlobalRequestObserver
import net.gotev.uploadservice.okhttp.OkHttpStack
import org.koin.android.ext.android.inject

class App : Application() {
    private val uploadObserverService: GlobalUploadObserverService by inject()

    companion object {
        const val UPLOADING_NOTIFICATION_CHANNEL_ID = "StationPhotoVerificationUploadService"
        const val UPLOADING_NOTIFICATION_CHANNEL_NAME = "Station Photo Verification Upload Service"
    }

    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(
            UPLOADING_NOTIFICATION_CHANNEL_ID,
            UPLOADING_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        UploadServiceConfig.initialize(
            context = this,
            defaultNotificationChannel = UPLOADING_NOTIFICATION_CHANNEL_ID,
            debug = BuildConfig.DEBUG
        )
        UploadServiceConfig.httpStack = OkHttpStack()
        UploadServiceLogger.setLogLevel(UploadServiceLogger.LogLevel.Debug)
        GlobalRequestObserver(this, uploadObserverService)
    }
}
