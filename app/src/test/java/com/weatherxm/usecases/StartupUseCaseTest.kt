package com.weatherxm.usecases

import com.weatherxm.TestConfig.context
import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.UserPreferencesRepository
import com.weatherxm.service.workers.RefreshFcmApiWorker
import com.weatherxm.ui.startup.StartupState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

class StartupUseCaseTest : BehaviorSpec({
    val authRepo = mockk<AuthRepository>()
    val appConfigRepo = mockk<AppConfigRepository>()
    val userPreferencesRepo = mockk<UserPreferencesRepository>()
    val usecase =
        StartupUseCaseImpl(context, authRepo, userPreferencesRepo, appConfigRepo, Dispatchers.IO)

    beforeSpec {
        coJustRun { appConfigRepo.setLastRemindedVersion() }
        mockkObject(RefreshFcmApiWorker)
        justRun { RefreshFcmApiWorker.initAndRefreshToken(context, null) }
    }

    suspend fun getStartupType() = usecase.getStartupState().first()

    context("Get startup state") {
        given("Some repositories providing data regarding the startup state") {
            When("app should be updated") {
                every { appConfigRepo.shouldUpdate() } returns true
                then("return ShowUpdate") {
                    getStartupType().shouldBeTypeOf<StartupState.ShowUpdate>()
                }
                then("set the last reminded version") {
                    coVerify(exactly = 1) { appConfigRepo.setLastRemindedVersion() }
                }
            }
            When("app should not be updated") {
                every { appConfigRepo.shouldUpdate() } returns false
                and("user is logged in") {
                    every { authRepo.isLoggedIn() } returns true
                    and("we should show the opt-in analytics screen") {
                        every { userPreferencesRepo.shouldShowAnalyticsOptIn() } returns true
                        then("return ShowAnalyticsOptIn") {
                            getStartupType().shouldBeTypeOf<StartupState.ShowAnalyticsOptIn>()
                        }
                    }
                    and("we should show home") {
                        every { userPreferencesRepo.shouldShowAnalyticsOptIn() } returns false
                        then("return ShowHome") {
                            getStartupType().shouldBeTypeOf<StartupState.ShowHome>()
                        }
                    }
                    then("refresh the FCM token (exactly = 2 because we have 2 `and` conditions") {
                        coVerify(exactly = 2) {
                            RefreshFcmApiWorker.initAndRefreshToken(context, null)
                        }
                    }
                }
                and("User is logged out") {
                    every { authRepo.isLoggedIn() } returns false
                    then("return ShowExplorer") {
                        getStartupType().shouldBeTypeOf<StartupState.ShowExplorer>()
                    }
                }
            }
        }
    }
})
