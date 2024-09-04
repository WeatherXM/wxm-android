package com.weatherxm.data.repository

import com.weatherxm.data.datasource.AppConfigDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class AppConfigRepositoryTest : BehaviorSpec({
    val dataSource = mockk<AppConfigDataSource>()
    val repo = AppConfigRepositoryImpl(dataSource)

    val changelog = "changelog"
    val installationId = "installationId"

    beforeSpec {
        every { dataSource.getChangelog() } returns changelog
        every { dataSource.getInstallationId() } returns installationId
        justRun { dataSource.setLastRemindedVersion() }
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
        given("We get the installation ID from the data source") {
            then("return it") {
                repo.getInstallationId() shouldBe installationId
            }
        }
    }
})
