package com.weatherxm.ui.widgets.currentweather

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import arrow.core.Either
import arrow.core.getOrElse
import com.weatherxm.data.models.ApiError.GenericError.JWTError.ForbiddenError
import com.weatherxm.data.models.UserActionError.UserNotLoggedInError
import com.weatherxm.data.requireNetwork
import com.weatherxm.data.services.CacheService
import com.weatherxm.data.services.CacheService.Companion.KEY_CURRENT_WEATHER_WIDGET_IDS
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_ID
import com.weatherxm.ui.common.Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_ID
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_SHOULD_SELECT_STATION
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_TYPE
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.WidgetCurrentWeatherUseCase
import com.weatherxm.util.WidgetHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class CurrentWeatherWidgetWorkerUpdate(
    private val context: Context,
    private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {
    companion object {
        private const val UPDATE_INTERVAL_IN_MINS = 15L

        fun restartAllWorkers(context: Context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val ids = prefs.getStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, setOf())

            ids?.forEach {
                val deviceId =
                    prefs.getString(CacheService.getWidgetFormattedKey(it.toInt()), String.empty())
                if (!deviceId.isNullOrEmpty()) {
                    initAndStart(context, it.toInt(), deviceId)
                }
            }
        }

        fun initAndStart(context: Context, appWidgetId: Int, deviceId: String) {
            Timber.d("Updating Work Manager for widget [$appWidgetId].")
            val data = Data.Builder()
                .putInt(ARG_WIDGET_ID, appWidgetId)
                .putString(ARG_DEVICE_ID, deviceId)
                .build()

            val widgetUpdateRequest = PeriodicWorkRequestBuilder<CurrentWeatherWidgetWorkerUpdate>(
                UPDATE_INTERVAL_IN_MINS,
                TimeUnit.MINUTES
            ).setConstraints(Constraints.requireNetwork()).setInputData(data).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "CURRENT_WEATHER_UPDATE_WORK_$appWidgetId",
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                widgetUpdateRequest
            )
        }
    }

    private val widgetUseCase: WidgetCurrentWeatherUseCase by inject()
    private val authUseCase: AuthUseCase by inject()
    private val widgetHelper: WidgetHelper by inject()

    override suspend fun doWork(): Result {
        val widgetId = workerParams.inputData.getInt(ARG_WIDGET_ID, INVALID_APPWIDGET_ID)
        val deviceId = workerParams.inputData.getString(ARG_DEVICE_ID)
        val widgetType =
            widgetHelper.getWidgetTypeById(AppWidgetManager.getInstance(context), widgetId)
        val isWidgetActive =
            widgetHelper.getWidgetIds().getOrElse { mutableListOf() }.contains(widgetId.toString())
                && widgetType != null

        if (widgetId == INVALID_APPWIDGET_ID || deviceId.isNullOrEmpty() || !isWidgetActive) {
            Timber.d("Cancelling WorkManager for Widget [$widgetId].")
            WorkManager.getInstance(context).cancelWorkById(workerParams.id)
            return Result.failure()
        }

        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        intent.putExtra(ARG_IS_CUSTOM_APPWIDGET_UPDATE, true)
        intent.putExtra(ARG_WIDGET_TYPE, widgetType as Parcelable)

        return authUseCase.isLoggedIn().let {
            if (it) {
                widgetUseCase.getUserDevice(deviceId).map { device ->
                    Timber.d("Got device for [$widgetId].")
                    intent.putExtra(ARG_DEVICE, device)
                    context.sendBroadcast(intent)
                    Result.success()
                }
            } else {
                Either.Left(UserNotLoggedInError())
            }
        }.getOrElse { failure ->
            when (failure) {
                is UserNotLoggedInError -> Result.success()
                is ForbiddenError -> {
                    intent.putExtra(ARG_WIDGET_SHOULD_SELECT_STATION, true)
                    context.sendBroadcast(intent)
                    Result.success()
                }
                else -> {
                    Timber.w(
                        Exception("Fetching user device for widget failed: ${failure.code}"),
                        failure.toString()
                    )
                    Result.retry()
                }
            }
        }
    }
}
