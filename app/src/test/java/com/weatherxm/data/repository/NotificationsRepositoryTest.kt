package com.weatherxm.data.repository

import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.Failure
import com.weatherxm.data.datasource.AppConfigDataSource
import com.weatherxm.data.datasource.NotificationsDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

class NotificationsRepositoryTest : BehaviorSpec({
    val dataSource = mockk<NotificationsDataSource>()
    val appConfigDataSource = mockk<AppConfigDataSource>()
    val repo = NotificationsRepositoryImpl(dataSource, appConfigDataSource)

    val installationId = "installationId"
    val fcmToken = "fcmToken"
    val fcmTokenFromDataSource = "fcmTokenFromDataSource"

    beforeSpec {
        coEvery { dataSource.getFcmToken() } returns fcmTokenFromDataSource
        coMockEitherRight({ dataSource.setFcmToken(installationId, fcmToken) }, Unit)
        coMockEitherRight({ dataSource.setFcmToken(installationId, fcmTokenFromDataSource) }, Unit)
    }

    context("Set FCM Token") {
        given("A Data Source providing the installation ID") {
            When("installation ID is found") {
                every { appConfigDataSource.getInstallationId() } returns installationId
                and("we use an explicit FCM Token that we have as a param") {
                    then("call the respective function in data source") {
                        repo.setFcmToken(fcmToken).isSuccess(Unit)
                        coVerify(exactly = 0) { dataSource.getFcmToken() }
                        coVerify(exactly = 1) { dataSource.setFcmToken(installationId, fcmToken) }
                    }
                }
                and("The FCM Token we have as a param is null") {
                    repo.setFcmToken(null).isSuccess(Unit)
                    then("get the FCM token from the Data Source") {
                        coVerify(exactly = 1) { dataSource.getFcmToken() }
                    }
                    then("call the respective function in data source") {
                        coVerify(exactly = 1) {
                            dataSource.setFcmToken(
                                installationId,
                                fcmTokenFromDataSource
                            )
                        }
                    }
                }
            }
            When("installation ID is not found") {
                every { appConfigDataSource.getInstallationId() } returns null
                then("should return failure InstallationIdNotFound") {
                    repo.setFcmToken(fcmToken).leftOrNull()
                        .shouldBeTypeOf<Failure.InstallationIdNotFound>()
                }
            }
        }
    }

})
