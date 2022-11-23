package com.weatherxm.usecases

import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.ui.startup.StartupState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import org.koin.core.component.KoinComponent
import timber.log.Timber

interface StartupUseCase {
    fun getStartupState(): Flow<StartupState>
}

class StartupUseCaseImpl(
    private val authRepository: AuthRepository,
    private val appConfigRepository: AppConfigRepository
) : StartupUseCase, KoinComponent {

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
                authRepository.isLoggedIn()
                    .map {
                        Timber.d("Already logged in.")
                        trySend(StartupState.ShowHome)
                    }
                    .mapLeft {
                        Timber.d("Not logged in. Show explorer.")
                        trySend(StartupState.ShowExplorer)
                    }
            }
            awaitClose { /* Do nothing */ }
        }

        // Return a combined flow
        return combine(delayFlow, stateFlow) { _, state ->
            state
        }.flowOn(Dispatchers.IO)
    }
}
