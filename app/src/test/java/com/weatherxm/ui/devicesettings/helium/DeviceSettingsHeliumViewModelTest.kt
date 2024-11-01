package com.weatherxm.ui.devicesettings.helium

import android.text.format.DateFormat
import com.weatherxm.R
import com.weatherxm.TestConfig.context
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.HOUR_FORMAT_24H
import com.weatherxm.data.models.BatteryState
import com.weatherxm.data.models.DeviceInfo
import com.weatherxm.data.models.Firmware
import com.weatherxm.data.models.RewardSplit
import com.weatherxm.data.models.WeatherStation
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.BundleName
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.RewardSplitsData
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.devicesettings.UIDeviceInfo
import com.weatherxm.ui.devicesettings.UIDeviceInfoItem
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.StationSettingsUseCase
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceSettingsHeliumViewModelTest : BehaviorSpec({
    val settingsUseCase = mockk<StationSettingsUseCase>()
    val userUseCase = mockk<UserUseCase>()
    val authUseCase = mockk<AuthUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: DeviceSettingsHeliumViewModel

    val device = UIDevice(
        "deviceId",
        "My Weather Station",
        String.empty(),
        DeviceRelation.OWNED,
        "la:bel",
        "friendlyName",
        BundleName.h1,
        "H1",
        null,
        "WS2000",
        null,
        null,
        null,
        null,
        null,
        true,
        "1.0.0",
        "1.1.0",
        ZonedDateTime.parse("2024-10-01T14:00:00+02:00"),
        ZonedDateTime.parse("2024-10-01T14:00:00+02:00"),
        null,
        null,
        null,
        true,
        null,
        null,
        null,
        null,
        null
    )
    val stakeholderSplits = RewardSplitsData(
        listOf(RewardSplit("walletAddress", 50, 50F), RewardSplit("wallet", 50, 50F)),
        "walletAddress"
    )
    val deviceInfo = DeviceInfo(
        "",
        ZonedDateTime.parse("2024-10-01T14:00:00+02:00"),
        stakeholderSplits.splits,
        null,
        WeatherStation(
            "WS2000",
            Firmware("1.0.0", "1.0.0"),
            ZonedDateTime.parse("2024-10-01T14:00:00+02:00"),
            "devEUI",
            "1.0.0",
            "last-hotspot-seen",
            ZonedDateTime.parse("2024-10-01T14:00:00+02:00"),
            "-100",
            ZonedDateTime.parse("2024-10-01T14:00:00+02:00"),
            -100,
            ZonedDateTime.parse("2024-10-01T14:00:00+02:00"),
            BatteryState.low
        )
    )
    val deviceInfoFromDevice = mutableListOf(
        UIDeviceInfoItem("Station Name", "My Weather Station", null),
        UIDeviceInfoItem("Bundle Name", "H1", null),
        UIDeviceInfoItem("Model", "WS2000"),
        UIDeviceInfoItem("Claimed At", "Oct 1, 2024, 14:00", null),
    )

    val uiFullDeviceInfo = UIDeviceInfo(
        mutableListOf(
            UIDeviceInfoItem("Station Name", "My Weather Station", null),
            UIDeviceInfoItem("Bundle Name", "H1", null),
            UIDeviceInfoItem("Model", "WS2000"),
            UIDeviceInfoItem("Claimed At", "Oct 1, 2024, 14:00", null),
            UIDeviceInfoItem("Dev EUI", "devEUI"),
            UIDeviceInfoItem(
                "Firmware Version",
                "1.0.0 ➞ 1.1.0",
                DeviceAlert.createWarning(DeviceAlertType.NEEDS_UPDATE)
            ),
            UIDeviceInfoItem("Hardware Version", "1.0.0"),
            UIDeviceInfoItem(
                "Battery Level",
                "Low",
                DeviceAlert.createWarning(DeviceAlertType.LOW_BATTERY)
            ),
            UIDeviceInfoItem("Last Hotspot", "Last Hotspot Seen @ Oct 1, 2024, 14:00"),
            UIDeviceInfoItem("TX RSSI", "-100 dBm @ Oct 1, 2024"),
            UIDeviceInfoItem("Last Station Activity", "Oct 1, 2024, 14:00"),
        ),
        mutableListOf(),
        mutableListOf(),
        stakeholderSplits
    )

    listener(InstantExecutorListener())
    Dispatchers.setMain(StandardTestDispatcher())

    beforeSpec {
        startKoin {
            modules(
                module {
                    single<DateTimeFormatter>(named(HOUR_FORMAT_24H)) {
                        DateTimeFormatter.ofPattern(HOUR_FORMAT_24H)
                    }
                    single<Resources> {
                        resources
                    }
                }
            )
        }
        justRun { analytics.trackEventFailure(any()) }
        every { DateFormat.is24HourFormat(context) } returns true
        coMockEitherRight({ authUseCase.isLoggedIn() }, true)
        coMockEitherRight({ userUseCase.getWalletAddress() }, "walletAddress")
        every { settingsUseCase.shouldNotifyOTA(device) } returns true

        every { resources.getString(R.string.station_default_name) } returns "Station Name"
        every { resources.getString(R.string.bundle_name) } returns "Bundle Name"
        every { resources.getString(R.string.claimed_at) } returns "Claimed At"
        every { resources.getString(R.string.model) } returns "Model"
        every { resources.getString(R.string.dev_eui) } returns "Dev EUI"
        every { resources.getString(R.string.rssi, any(), any()) } returns "-100 dBm @ Oct 1, 2024"
        every { resources.getString(R.string.hardware_version) } returns "Hardware Version"
        every { resources.getString(R.string.last_hotspot) } returns "Last Hotspot"
        every { resources.getString(R.string.last_tx_rssi) } returns "TX RSSI"
        every { resources.getString(R.string.battery_level) } returns "Battery Level"
        every { resources.getString(R.string.battery_level_low) } returns "Low"
        every {
            resources.getString(R.string.last_weather_station_activity)
        } returns "Last Station Activity"
        every { resources.getString(R.string.latest_hint) } returns "(latest)"
        every { resources.getString(R.string.firmware_version) } returns "Firmware Version"

        viewModel = DeviceSettingsHeliumViewModel(
            device,
            settingsUseCase,
            userUseCase,
            authUseCase,
            resources,
            analytics
        )
    }

    context("Get Device Info") {
        given("a usecase returning the result of getting device info") {
            When("it's a failure") {
                coMockEitherLeft({ settingsUseCase.getDeviceInfo(device.id) }, failure)
                runTest { viewModel.getDeviceInformation(context) }
                then("LiveData onDeviceInfo posts the UIDeviceInfo created by the Device object") {
                    viewModel.onDeviceInfo().value shouldBe UIDeviceInfo(
                        deviceInfoFromDevice,
                        mutableListOf(),
                        mutableListOf(),
                        null
                    )
                }
                then("analytics should track the event's failure") {
                    verify(exactly = 1) { analytics.trackEventFailure(any()) }
                }
                then("LiveData onLoading should have the value false") {
                    viewModel.onLoading().value shouldBe false
                }
            }
            When("it's a success") {
                coMockEitherRight({ settingsUseCase.getDeviceInfo(device.id) }, deviceInfo)
                runTest { viewModel.getDeviceInformation(context) }
                then("LiveData onDeviceInfo should post the UIDeviceInfo created") {
                    viewModel.onDeviceInfo().value shouldBe uiFullDeviceInfo
                }
                then("LiveData onLoading should have the value false") {
                    viewModel.onLoading().value shouldBe false
                }
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
        stopKoin()
    }
})
