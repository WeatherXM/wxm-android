package com.weatherxm.data.datasource

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.weatherxm.TestConfig.successUnitResponse
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.data.network.ApiService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot

class NotificationsDataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val firebaseMessaging = mockk<FirebaseMessaging>()
    val dataSource = NotificationsDataSourceImpl(apiService, firebaseMessaging)

    val mockGetTokenTask = mockk<Task<String>>()
    val slot = slot<OnCompleteListener<String>>()

    val installationId = "installationId"
    val fcmToken = "fcmToken"

    beforeSpec {
        mockkStatic("com.google.firebase.messaging.FirebaseMessaging")
        every { mockGetTokenTask.addOnCompleteListener(capture(slot)) } answers {
            slot.captured.onComplete(mockGetTokenTask)
            mockGetTokenTask
        }
        every { mockGetTokenTask.isComplete } returns true
        every { mockGetTokenTask.isCanceled } returns false
        every { mockGetTokenTask.exception } returns null
        every { mockGetTokenTask.result } returns fcmToken
        every { firebaseMessaging.token } returns mockGetTokenTask
    }

    context("Get / Set FCM Token") {
        When("We want to get the FCM Token") {
            then("return it") {
                dataSource.getFcmToken() shouldBe fcmToken
            }
        }
        When("We want to set the FCM Token") {
            testNetworkCall(
                "fcm token",
                Unit,
                successUnitResponse,
                mockFunction = { apiService.setFcmToken(installationId, fcmToken) },
                runFunction = { dataSource.setFcmToken(installationId, fcmToken) }
            )
        }
    }
})
