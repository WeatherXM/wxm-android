package com.weatherxm.ui.startup

import android.content.Intent
import com.weatherxm.data.models.RemoteMessageType
import com.weatherxm.data.models.WXMRemoteMessage
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_ID
import com.weatherxm.ui.common.Contracts.ARG_URL
import com.weatherxm.usecases.StartupUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class StartupViewModelTest : BehaviorSpec({
    val usecase = mockk<StartupUseCase>()
    val viewModel = StartupViewModel(usecase)

    val intent = mockk<Intent>()
    val startupState = StartupState.ShowHome
    val startupFlow: Flow<StartupState> = flowOf(startupState)

    val url = "url"
    val startupStateAnnouncement = StartupState.ShowDeepLinkRouter(
        WXMRemoteMessage(RemoteMessageType.ANNOUNCEMENT, url = url)
    )

    val deviceId = "deviceId"
    val startupStateStation = StartupState.ShowDeepLinkRouter(
        WXMRemoteMessage(RemoteMessageType.STATION, deviceId = deviceId)
    )

    listener(InstantExecutorListener())
    Dispatchers.setMain(StandardTestDispatcher())

    beforeSpec {
        every { usecase.getStartupState() } returns startupFlow
    }

    context("Get Startup State") {
        When("The intent doesn't have any data with a specific TYPE") {
            every { intent.hasExtra(Contracts.ARG_TYPE) } returns false
            then("get the default startup from the usecase") {
                runTest { viewModel.handleStartup(intent) }
                viewModel.onStartupState().value shouldBe startupState
            }
        }
        When("The intent has data associated with a specific TYPE") {
            every { intent.hasExtra(Contracts.ARG_TYPE) } returns true
            and("The ARG_TYPE extra is not a valid string extra") {
                every { intent.getStringExtra(Contracts.ARG_TYPE) } returns null
                then("get the default startup from the usecase") {
                    runTest { viewModel.handleStartup(intent) }
                    viewModel.onStartupState().value shouldBe startupState
                }
            }
            and("The ARG_TYPE extra is an ANNOUNCEMENT") {
                every {
                    intent.getStringExtra(Contracts.ARG_TYPE)
                } returns RemoteMessageType.ANNOUNCEMENT.id
                every { intent.getStringExtra(ARG_URL) } returns url
                then("get the ShowDeepLinkRouter state with the correct args") {
                    runTest { viewModel.handleStartup(intent) }
                    viewModel.onStartupState().value shouldBe startupStateAnnouncement
                }
            }
            and("The ARG_TYPE extra is a STATION") {
                every {
                    intent.getStringExtra(Contracts.ARG_TYPE)
                } returns RemoteMessageType.STATION.id
                every { intent.getStringExtra(ARG_DEVICE_ID) } returns deviceId
                then("get the ShowDeepLinkRouter state with the correct args") {
                    runTest { viewModel.handleStartup(intent) }
                    viewModel.onStartupState().value shouldBe startupStateStation
                }
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
    }
})
