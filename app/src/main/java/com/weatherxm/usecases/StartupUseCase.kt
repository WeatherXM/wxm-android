package com.weatherxm.usecases

import android.content.Context
import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.UserPreferencesRepository
import com.weatherxm.service.workers.RefreshFcmApiWorker
import com.weatherxm.ui.startup.StartupState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import timber.log.Timber

interface StartupUseCase {
    fun getStartupState(): Flow<StartupState>
}

class StartupUseCaseImpl(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appConfigRepository: AppConfigRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : StartupUseCase {

    companion object {
        private const val MIN_KEEP_ON_TIME = 1000L
    }

    override fun getStartupState(): Flow<StartupState> {
        // A flow that delays the Splash Screen long enough for the animation to complete
        val delayFlow = flowOf(Unit).onStart { delay(MIN_KEEP_ON_TIME) }

        // A flow that fetches the startup state
        val stateFlow = callbackFlow {
            if (appConfigRepository.shouldUpdate()) {
                appConfigRepository.setLastRemindedVersion()
                trySend(StartupState.ShowUpdate)
            } else {
                val isLoggedIn = authRepository.isLoggedIn().apply {
                    if (this) RefreshFcmApiWorker.initAndRefreshToken(context, null)
                }
                Timber.d("User logged in: $isLoggedIn")
                if (isLoggedIn && userPreferencesRepository.shouldShowAnalyticsOptIn()) {
                    Timber.d("Show the Analytics Opt-In screen.")
                    trySend(StartupState.ShowAnalyticsOptIn)
                } else if (isLoggedIn) {
                    Timber.d("Show the Home screen.")
                    trySend(StartupState.ShowHome)
                } else {
                    Timber.d("Show the Explorer.")
                    trySend(StartupState.ShowExplorer)
                }
            }
            awaitClose { /* Do nothing */ }
        }

        // Return a combined flow
        return combine(delayFlow, stateFlow) { _, state ->
            state
        }.flowOn(dispatcher)
    }
}
