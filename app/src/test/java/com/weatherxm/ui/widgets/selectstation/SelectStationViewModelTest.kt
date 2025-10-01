package com.weatherxm.ui.widgets.selectstation

import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.WidgetSelectStationUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class SelectStationViewModelTest : BehaviorSpec({
    val usecase = mockk<WidgetSelectStationUseCase>()
    val authUseCase = mockk<AuthUseCase>()
    lateinit var viewModel: SelectStationViewModel

    val deviceId = "deviceId"
    val devices = listOf(
        UIDevice(
            deviceId,
            String.empty(),
            String.empty(),
            DeviceRelation.OWNED,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
    )
    val widgetId = 0

    listener(InstantExecutorListener())

    beforeSpec {
        startKoin {
            modules(
                module {
                    single<Resources> {
                        resources
                    }
                }
            )
        }
        justRun { usecase.saveWidgetData(widgetId, deviceId) }

        viewModel = SelectStationViewModel(usecase, authUseCase, dispatcher)
    }

    context("GET / SET the selected station") {
        When("GET the default station") {
            then("return the default empty station") {
                viewModel.getStationSelected() shouldBe UIDevice.empty()
            }
        }
        When("SET a new station") {
            viewModel.setStationSelected(devices[0])
            then("GET it to ensure it has been set") {
                viewModel.getStationSelected() shouldBe devices[0]
            }
        }
    }

    context("Fetch devices") {
        given("a usecase returning the list of the devices") {
            When("it's failure") {
                coMockEitherLeft({ usecase.getUserDevices() }, failure)
                runTest { viewModel.fetch() }
                then("LiveData onDevices should post the failure with a generic message") {
                    viewModel.onDevices().isError(REACH_OUT_MSG)
                }
            }
            When("it's a success") {
                coMockEitherRight({ usecase.getUserDevices() }, devices)
                runTest { viewModel.fetch() }
                then("LiveData should post the updated devices value") {
                    viewModel.onDevices().isSuccess(devices)
                }
            }
        }
    }

    context("Check if the user is logged in and proceed accordingly") {
        given("a usecase returning if the user is logged in or not") {
            When("the user is logged in") {
                every { authUseCase.isLoggedIn() } returns true
                coMockEitherRight({ usecase.getUserDevices() }, emptyList<UIDevice>())

                runTest { viewModel.checkIfLoggedInAndProceed() }
                then("fetching of the devices should take place (mocked as empty list)") {
                    viewModel.onDevices().isSuccess(emptyList())
                }
            }
            When("the user is NOT logged in") {
                every { authUseCase.isLoggedIn() } returns false
                runTest { viewModel.checkIfLoggedInAndProceed() }
                then("LiveData isNotLoggedIn should be called with value Unit") {
                    viewModel.isNotLoggedIn().value shouldBe Unit
                }
            }
        }
    }

    context("Save widget data") {
        given("a widget ID") {
            then("save its data") {
                viewModel.saveWidgetData(widgetId)
                verify(exactly = 1) { usecase.saveWidgetData(widgetId, deviceId) }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
