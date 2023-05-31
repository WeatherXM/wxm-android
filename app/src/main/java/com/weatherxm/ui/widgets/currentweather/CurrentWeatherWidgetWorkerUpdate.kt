package com.weatherxm.ui.widgets.currentweather

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.right
import com.weatherxm.data.UserActionError.UserNotLoggedInError
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_ID
import com.weatherxm.ui.common.Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_ID
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_TYPE
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.WidgetCurrentWeatherUseCase
import com.weatherxm.util.WidgetHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class CurrentWeatherWidgetWorkerUpdate(
    private val context: Context,
    private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {
    companion object {
        const val UPDATE_INTERVAL_IN_MINS = 15L
    }

    private val widgetUseCase: WidgetCurrentWeatherUseCase by inject()
    private val authUseCase: AuthUseCase by inject()
    private val widgetHelper: WidgetHelper by inject()

    override suspend fun doWork(): Result {
        val widgetId = workerParams.inputData.getInt(ARG_WIDGET_ID, INVALID_APPWIDGET_ID)
        val deviceId = workerParams.inputData.getString(ARG_DEVICE_ID)
        val isWidgetActive = widgetHelper.getWidgetIds()
            .getOrElse { mutableListOf() }
            .contains(widgetId.toString())

        if (widgetId == INVALID_APPWIDGET_ID || deviceId.isNullOrEmpty() || !isWidgetActive) {
            Timber.d("Cancelling WorkManager for Widget [$widgetId].")
            WorkManager.getInstance(context).cancelWorkById(workerParams.id)
            return Result.failure()
        }

        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        intent.putExtra(ARG_IS_CUSTOM_APPWIDGET_UPDATE, true)
        intent.putExtra(
            ARG_WIDGET_TYPE,
            widgetHelper.getWidgetTypeById(AppWidgetManager.getInstance(context), widgetId)
        )

        return authUseCase.isLoggedIn()
            .flatMap { isLoggedIn ->
                if (isLoggedIn) {
                    true.right()
                } else {
                    Either.Left(UserNotLoggedInError())
                }
            }
            .flatMap {
                widgetUseCase.getUserDevice(deviceId).map { device ->
                    Timber.d("Got device for [$widgetId].")
                    intent.putExtra(ARG_DEVICE, device)
                    context.sendBroadcast(intent)
                    Result.success()
                }
            }
            .getOrElse { failure ->
                Timber.w(
                    Exception("Fetching user device for widget failed: ${failure.code}"),
                    failure.toString()
                )
                if (failure is UserNotLoggedInError) Result.success() else Result.retry()
            }
    }
}
