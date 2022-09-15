package com.weatherxm.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Exposes a flow that emits in predefined intervals. Can be used in flows for timers, etc.
 */
class RefreshHandler(
    val refreshIntervalMillis: Long = 5000L
) {
    fun flow() = flow {
        Timber.d("A refresh task is starting")
        while (true) {
            Timber.d("A refresh event has happened")
            emit(Unit)
            delay(refreshIntervalMillis)
        }
    }
}
