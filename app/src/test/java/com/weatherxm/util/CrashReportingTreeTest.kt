package com.weatherxm.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import timber.log.Timber

class CrashReportingTreeTest : BehaviorSpec({
    lateinit var crashlytics: FirebaseCrashlytics
    val testMessage = "This is a test message"
    val testException = Exception("This is a test exception")

    beforeContainer {
        crashlytics = mockk<FirebaseCrashlytics>()
        Timber.plant(CrashReportingTree(crashlytics))
        justRun { crashlytics.log(any()) }
        justRun { crashlytics.recordException(any()) }
    }

    given("CrashReportingTree planted in Timber") {
        When("a DEBUG log is made") {
            Timber.d(testMessage)
            then("the crashlytics log should not be called") {
                verify(exactly = 0) { crashlytics.log(any()) }
            }
        }
        When("a WARN log is made") {
            Timber.w(testMessage)
            then("the crashlytics log should be called") {
                verify(exactly = 1) { crashlytics.log(testMessage) }
            }
        }
        When("a ERROR log is made") {
            Timber.e(testMessage)
            then("the crashlytics log should be called") {
                verify(exactly = 1) { crashlytics.log(testMessage) }
            }
        }
        When("An EXCEPTION is reported with either WARN or ERROR log") {
            Timber.w(testException)
            Timber.e(testException)
            then("the crashlytics recordException should be called") {
                verify(exactly = 2) { crashlytics.recordException(testException) }
            }
        }
    }
})
