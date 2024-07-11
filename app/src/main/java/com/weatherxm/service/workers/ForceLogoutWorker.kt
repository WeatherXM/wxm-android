package com.weatherxm.service.workers

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.NotificationsRepository
import com.weatherxm.ui.common.Contracts
import com.weatherxm.util.WidgetHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ForceLogoutWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {
    private val authRepository: AuthRepository by inject()
    private val widgetHelper: WidgetHelper by inject()
    private val notificationsRepository: NotificationsRepository by inject()

    override suspend fun doWork(): Result {
        Timber.d("Starting Work Manager for forced logout.")
        notificationsRepository.deleteFcmToken()
        authRepository.logout()
        widgetHelper.getWidgetIds().onRight {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            val ids = it.map { id ->
                id.toInt()
            }
            intent.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                ids.toIntArray()
            )
            intent.putExtra(Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE, true)
            intent.putExtra(Contracts.ARG_WIDGET_SHOULD_LOGIN, true)
            context.sendBroadcast(intent)
        }
        return Result.success()
    }
}
