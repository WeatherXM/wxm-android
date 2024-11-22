package com.weatherxm.data.datasource

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.weatherxm.BuildConfig
import com.weatherxm.TestConfig.cacheService
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.data.datasource.AppConfigDataSourceImpl.Companion.REMOTE_CONFIG_CHANGELOG
import com.weatherxm.data.datasource.AppConfigDataSourceImpl.Companion.REMOTE_CONFIG_MINIMUM_VERSION_CODE
import com.weatherxm.data.datasource.AppConfigDataSourceImpl.Companion.REMOTE_CONFIG_VERSION_CODE
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify

class AppConfigDataSourceTest : BehaviorSpec({
    val remoteConfig = mockk<FirebaseRemoteConfig>()
    val firebaseInstallations = mockk<FirebaseInstallations>()

    val dataSource = AppConfigDataSourceImpl(remoteConfig, firebaseInstallations, cacheService)

    val mockGetIdTask = mockk<Task<String>>()
    val slot = slot<OnCompleteListener<String>>()

    val changelog = "changelog"
    val installationId = "installationId"
    val customVersionCode = 100

    beforeSpec {
        every { remoteConfig.getString(REMOTE_CONFIG_CHANGELOG) } returns changelog
        every { cacheService.getLastRemindedVersion() } returns 0
        justRun { cacheService.setLastRemindedVersion(customVersionCode) }
        justRun { cacheService.setInstallationId(installationId) }

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

    context("Get if the app should be updated") {
        When("The current version is lower than the remote config version") {
            every {
                remoteConfig.getDouble(REMOTE_CONFIG_VERSION_CODE)
            } returns BuildConfig.VERSION_CODE + 1.0
            then("should return true") {
                dataSource.shouldUpdate() shouldBe true
            }
        }
        When("The current version is equal to the remote config version") {
            every {
                remoteConfig.getDouble(REMOTE_CONFIG_VERSION_CODE)
            } returns BuildConfig.VERSION_CODE.toDouble()
            then("should return true") {
                dataSource.shouldUpdate() shouldBe false
            }
        }
        When("The current version is higher than the remote config version") {
            every {
                remoteConfig.getDouble(REMOTE_CONFIG_VERSION_CODE)
            } returns BuildConfig.VERSION_CODE.toDouble() - 1.0
            then("should return true") {
                dataSource.shouldUpdate() shouldBe false
            }
        }
    }

    context("Get if an update is mandatory") {
        When("The current version is lower than the remote config minimum version") {
            every {
                remoteConfig.getDouble(REMOTE_CONFIG_MINIMUM_VERSION_CODE)
            } returns BuildConfig.VERSION_CODE + 1.0
            then("should return true") {
                dataSource.isUpdateMandatory() shouldBe true
            }
        }
        When("The current version is equal to the remote config minimum version") {
            every {
                remoteConfig.getDouble(REMOTE_CONFIG_MINIMUM_VERSION_CODE)
            } returns BuildConfig.VERSION_CODE.toDouble()
            then("should return true") {
                dataSource.isUpdateMandatory() shouldBe false
            }
        }
        When("The current version is higher than the remote config minimum version") {
            every {
                remoteConfig.getDouble(REMOTE_CONFIG_MINIMUM_VERSION_CODE)
            } returns BuildConfig.VERSION_CODE.toDouble() - 1.0
            then("should return true") {
                dataSource.isUpdateMandatory() shouldBe false
            }
        }
    }

    context("Get the update's changelog") {
        given("the firebase remote config source") {
            then("should return the changelog") {
                dataSource.getChangelog() shouldBe changelog
            }
        }
    }

    context("Get / Set last reminded version") {
        When("We want to get the last reminded version") {
            then("should return it") {
                dataSource.getLastRemindedVersion() shouldBe 0
            }
        }
        When("We want to set the last reminded version") {
            every {
                remoteConfig.getDouble(REMOTE_CONFIG_VERSION_CODE)
            } returns customVersionCode.toDouble()
            then("should save it in cache") {
                dataSource.setLastRemindedVersion()
                verify(exactly = 1) { cacheService.setLastRemindedVersion(customVersionCode) }
            }
        }
    }

    context("Get / Set installation ID") {
        When("We want to get the installation ID") {
            When("it's in cache") {
                coMockEitherRight({ cacheService.getInstallationId() }, installationId)
                then("return it") {
                    dataSource.getInstallationId() shouldBe installationId
                }
            }
            When("it's not in cache") {
                coMockEitherLeft({ cacheService.getInstallationId() }, failure)
                and("We get the installation ID from firebase installations") {
                    every { mockGetIdTask.exception } returns null
                    then("return it") {
                        dataSource.getInstallationId() shouldBe installationId
                        verify(exactly = 1) { dataSource.setInstallationId(installationId) }
                    }
                    then("save it in cache") {
                        verify(exactly = 1) { cacheService.setInstallationId(installationId) }
                    }
                }
                and("We cannot get the installation ID from firebase installations") {
                    every { mockGetIdTask.exception } returns Exception()
                    then("return null") {
                        dataSource.getInstallationId() shouldBe null
                    }
                }
            }
        }
    }
})
