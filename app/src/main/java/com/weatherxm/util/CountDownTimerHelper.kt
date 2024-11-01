package com.weatherxm.util

import android.os.CountDownTimer
import timber.log.Timber

class CountDownTimerHelper {
    companion object {
        const val COUNTDOWN_DURATION = 5000L
        const val COUNTDOWN_INTERVAL = 50L
    }

    private var timer: CountDownTimer? = null

    fun start(onProgress: (Int) -> Unit, onFinished: () -> Unit) {
        timer = object : CountDownTimer(COUNTDOWN_DURATION, COUNTDOWN_INTERVAL) {
            override fun onTick(msUntilDone: Long) {
                val progress =
                    ((COUNTDOWN_DURATION - msUntilDone) * 100L / COUNTDOWN_DURATION).toInt()
                Timber.d("Timer progress: $progress")
                onProgress(progress)
            }

            override fun onFinish() {
                onFinished()
            }
        }
        timer?.start()
    }

    fun stop() = timer?.cancel()
}
