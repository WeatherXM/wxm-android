package com.weatherxm.ui

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec

/**
 * Based on
 * 1. https://gist.github.com/OliverCulleyDeLange/84aa4d2b299b2dfff3746bfdf346cd3e
 * 2. https://jeroenmols.com/blog/2019/01/17/livedatajunit5/
 */
class InstantExecutorListener : TestListener {
    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
        })
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
}
