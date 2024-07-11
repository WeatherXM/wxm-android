package com.weatherxm.service.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.weatherxm.data.repository.NotificationsRepository
import com.weatherxm.data.requireNetwork
import com.weatherxm.ui.common.Contracts.ARG_FCM_TOKEN
import com.weatherxm.usecases.AuthUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class RefreshFcmApiWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {
    companion object {
        fun initAndRefreshToken(context: Context, token: String?) {
            Timber.d("Starting Work Manager for FCM [$token].")
            val data = Data.Builder()
                .putString(ARG_FCM_TOKEN, token)
                .build()

            val updateTokenWork = OneTimeWorkRequestBuilder<RefreshFcmApiWorker>()
                .setConstraints(Constraints.requireNetwork())
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueue(updateTokenWork)
        }
    }

    private val authUseCase: AuthUseCase by inject()
    private val notificationsRepository: NotificationsRepository by inject()

    override suspend fun doWork(): Result {
        return authUseCase.isLoggedIn().fold({ Result.failure() }, {
            notificationsRepository.setFcmToken(workerParams.inputData.getString(ARG_FCM_TOKEN))
            Result.success()
        })
    }
}
