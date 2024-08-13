package com.weatherxm.data.repository

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.installations.FirebaseInstallations
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.mockEitherRight
import com.weatherxm.data.datasource.AppConfigDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify

class AppConfigRepositoryTest : BehaviorSpec({
    val dataSource = mockk<AppConfigDataSource>()
    val firebaseInstallations = mockk<FirebaseInstallations>()
    val repo = AppConfigRepositoryImpl(dataSource, firebaseInstallations)

    val changelog = "changelog"
    val installationId = "installationId"
    val mockGetIdTask = mockk<Task<String>>()
    val slot = slot<OnCompleteListener<String>>()

    beforeSpec {
        every { dataSource.getChangelog() } returns changelog
        justRun { dataSource.setLastRemindedVersion() }
        justRun { dataSource.setInstallationId(installationId) }
        mockkStatic("com.google.firebase.installations.FirebaseInstallations")
        every { mockGetIdTask.addOnCompleteListener(capture(slot)) } answers {
            slot.captured.onComplete(mockGetIdTask)
            mockGetIdTask
        }
        every { mockGetIdTask.isComplete } returns true
        every { mockGetIdTask.isCanceled } returns false
        every { mockGetIdTask.result } returns installationId
        every { firebaseInstallations.id } returns mockGetIdTask
    }

    context("Get app update related information") {
        When("there is an update available") {
            every { dataSource.shouldUpdate() } returns true
            and("The last version we prompted for an update is lower than the new version") {
                every { dataSource.getLastRemindedVersion() } returns 0
                every { dataSource.getLastRemoteVersionCode() } returns 1
                then("return true") {
                    repo.shouldUpdate() shouldBe true
                }
                then("Get changelog") {
                    repo.getChangelog() shouldBe changelog
                }
                then("Set last reminded version") {
                    repo.setLastRemindedVersion()
                    verify(exactly = 1) { dataSource.setLastRemindedVersion() }
                }
            }
            and("The last version we prompted for an update is equal than the new version") {
                every { dataSource.getLastRemindedVersion() } returns 1
                every { dataSource.getLastRemoteVersionCode() } returns 1
                and("Update is mandatory") {
                    every { dataSource.isUpdateMandatory() } returns true
                    then("return true") {
                        repo.shouldUpdate() shouldBe true
                    }
                }
                and("Update is not mandatory") {
                    every { dataSource.isUpdateMandatory() } returns false
                    then("return false") {
                        repo.shouldUpdate() shouldBe false
                    }
                }
            }
        }
        When("there is no update available") {
            every { dataSource.shouldUpdate() } returns false
            then("return false") {
                repo.shouldUpdate() shouldBe false
            }
        }
    }

    context("Get installation ID") {
        When("We get the installation ID from the data source") {
            mockEitherRight({ dataSource.getInstallationId() }, installationId)
            then("return it") {
                repo.getInstallationId() shouldBe installationId
            }
        }
        When("We cannot get the installation ID from the data source") {
            coMockEitherLeft({ dataSource.getInstallationId() }, mockk())
            and("We get the installation ID from firebase installations") {
                every { mockGetIdTask.exception } returns null
                then("return it") {
                    repo.getInstallationId() shouldBe installationId
                    verify(exactly = 1) { dataSource.setInstallationId(installationId) }
                }
            }
            and("We cannot get the installation ID from firebase installations") {
                every { mockGetIdTask.exception } returns Exception()
                then("return null") {
                    repo.getInstallationId() shouldBe null
                }
            }
        }
    }
})
