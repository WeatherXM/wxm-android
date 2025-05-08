package com.weatherxm.ui.devicesettings.wifi

import android.text.format.DateFormat
import com.weatherxm.R
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.context
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.HOUR_FORMAT_24H
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.BatteryState
import com.weatherxm.data.models.DeviceInfo
import com.weatherxm.data.models.Firmware
import com.weatherxm.data.models.Gateway
import com.weatherxm.data.models.RewardSplit
import com.weatherxm.data.models.SeverityLevel
import com.weatherxm.data.models.WeatherStation
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.BundleName
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.RewardSplitsData
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.devicesettings.UIDeviceInfo
import com.weatherxm.ui.devicesettings.UIDeviceInfoItem
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.DevicePhotoUseCase
import com.weatherxm.usecases.StationSettingsUseCase
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class DeviceSettingsWifiViewModelTest : BehaviorSpec({
    val settingsUseCase = mockk<StationSettingsUseCase>()
    val photosUseCase = mockk<DevicePhotoUseCase>()
    val userUseCase = mockk<UserUseCase>()
    val authUseCase = mockk<AuthUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: DeviceSettingsWifiViewModel

    val device = UIDevice(
        "deviceId",
        "My Weather Station",
        String.empty(),
        DeviceRelation.OWNED,
        "la:bel",
        "friendlyName",
        BundleName.d1,
        "D1",
        null,
        "WS1001",
        "WG1200",
        null,
        null,
        null,
        null,
        null,
        true,
        "1.0.0",
        "1.0.0",
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
    val newFriendlyName = "newFriendlyName"
    val stakeholderSplits = RewardSplitsData(
        listOf(RewardSplit("walletAddress", 50F, 50F), RewardSplit("wallet", 50F, 50F)),
        "walletAddress"
    )
    val nonStakeholderSplits = RewardSplitsData(
        listOf(RewardSplit("wallet", 50F, 50F), RewardSplit("wallet2", 50F, 50F)),
        "non-stakeholder-wallet"
    )
    val uiDeviceInfoToShare = UIDeviceInfo(
        mutableListOf(UIDeviceInfoItem("titleDefault", "valueDefault", null)),
        mutableListOf(
            UIDeviceInfoItem(
                "titleGateway",
                "valueGateway",
                DeviceAlert(DeviceAlertType.OFFLINE, SeverityLevel.ERROR)
            )
        ),
        mutableListOf(
            UIDeviceInfoItem(
                "titleStation",
                "valueStation",
                DeviceAlert(DeviceAlertType.LOW_BATTERY, SeverityLevel.WARNING)
            )
        ),
        null
    )
    val sharingText =
        "titleDefault: valueDefault\ntitleGateway: valueGateway\ntitleStation: valueStation"

    val deviceInfo = DeviceInfo(
        "",
        ZonedDateTime.parse("2024-10-01T14:00:00+02:00"),
        stakeholderSplits.splits,
        Gateway(
            "WG1200",
            Firmware("1.0.0", "1.0.0"),
            ZonedDateTime.parse("2024-10-01T14:00:00+02:00"),
            "serialNumber",
            "5",
            ZonedDateTime.parse("2024-10-01T14:00:00+02:00"),
            "-100",
            ZonedDateTime.parse("2024-10-01T14:00:00+02:00"),
        ),
        WeatherStation(
            "WS1001",
            null,
            ZonedDateTime.parse("2024-10-01T14:00:00+02:00"),
            null,
            "1.0.0",
            null,
            null,
            null,
            null,
            -100,
            ZonedDateTime.parse("2024-10-01T14:00:00+02:00"),
            BatteryState.low
        )
    )
    val deviceInfoFromDevice = mutableListOf(
        UIDeviceInfoItem("Station Name", "My Weather Station", null),
        UIDeviceInfoItem("Bundle Name", "D1", null),
        UIDeviceInfoItem("Claimed At", "Oct 1, 2024, 14:00", null)
    )

    val uiFullDeviceInfo = UIDeviceInfo(
        deviceInfoFromDevice,
        mutableListOf(
            UIDeviceInfoItem("Model", "WG1200"),
            UIDeviceInfoItem("Serial Number", "serialNumber"),
            UIDeviceInfoItem("Firmware Version", "1.0.0 (latest)"),
            UIDeviceInfoItem("GPS Satellites", "5 sats @ Oct 1, 2024"),
            UIDeviceInfoItem("Wifi RSSI", "-100 dBm @ Oct 1, 2024"),
            UIDeviceInfoItem("Last Activity", "Oct 1, 2024, 14:00")
        ),
        mutableListOf(
            UIDeviceInfoItem("Model", "WS1001"),
            UIDeviceInfoItem("Hardware Version", "1.0.0"),
            UIDeviceInfoItem(
                "Station RSSI",
                "-100 dBm @ Oct 1, 2024",
                DeviceAlert.createError(DeviceAlertType.LOW_STATION_RSSI)
            ),
            UIDeviceInfoItem(
                "Battery Level",
                "Low",
                DeviceAlert.createWarning(DeviceAlertType.LOW_BATTERY)
            ),
            UIDeviceInfoItem("Last Station Activity", "Oct 1, 2024, 14:00"),
        ),
        stakeholderSplits
    )
    val stationPhotos = arrayListOf(
        StationPhoto("remotePath", "localPath")
    )
    val uploadIds = listOf("uploadId")

    val invalidClaimIdFailure = ApiError.UserError.ClaimError.InvalidClaimId("")

    listener(InstantExecutorListener())

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
        justRun {
            analytics.trackEventViewContent(
                AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                success = any()
            )
        }
        every { DateFormat.is24HourFormat(context) } returns true
        every {
            resources.getString(R.string.error_invalid_device_identifier)
        } returns "invalidIdentifier"
        every { authUseCase.isLoggedIn() } returns true
        coMockEitherRight({ userUseCase.getWalletAddress() }, "walletAddress")

        every { resources.getString(R.string.station_default_name) } returns "Station Name"
        every { resources.getString(R.string.bundle_name) } returns "Bundle Name"
        every { resources.getString(R.string.claimed_at) } returns "Claimed At"
        every { resources.getString(R.string.model) } returns "Model"
        every { resources.getString(R.string.serial_number) } returns "Serial Number"
        every { resources.getString(R.string.gps_number_sats) } returns "GPS Satellites"
        every {
            resources.getString(R.string.satellites, any(), any())
        } returns "5 sats @ Oct 1, 2024"
        every { resources.getString(R.string.wifi_rssi) } returns "Wifi RSSI"
        every { resources.getString(R.string.rssi, any(), any()) } returns "-100 dBm @ Oct 1, 2024"
        every { resources.getString(R.string.last_gateway_activity) } returns "Last Activity"
        every { resources.getString(R.string.hardware_version) } returns "Hardware Version"
        every { resources.getString(R.string.station_gateway_rssi) } returns "Station RSSI"
        every { resources.getString(R.string.battery_level) } returns "Battery Level"
        every { resources.getString(R.string.battery_level_low) } returns "Low"
        every {
            resources.getString(R.string.last_weather_station_activity)
        } returns "Last Station Activity"
        every { resources.getString(R.string.latest_hint) } returns "(latest)"
        every { resources.getString(R.string.firmware_version) } returns "Firmware Version"
        coMockEitherRight(
            { photosUseCase.deleteDevicePhoto(device.id, stationPhotos[0].remotePath!!) },
            Unit
        )
        every { photosUseCase.getDevicePhotoUploadIds(device.id) } returns uploadIds
        justRun { photosUseCase.retryUpload(device.id) }

        viewModel = DeviceSettingsWifiViewModel(
            device,
            settingsUseCase,
            photosUseCase,
            userUseCase,
            authUseCase,
            resources,
            analytics,
            dispatcher
        )
    }

    fun testFriendlyNameAnalyticsEvent(success: Long, verifyNumberOfEvents: Int) {
        verify(exactly = verifyNumberOfEvents) {
            analytics.trackEventViewContent(
                AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                success = success
            )
        }
    }

    context("Set and then clear the friendly name") {
        given("a friendly name to set") {
            When("it's not null and it's empty") {
                runTest { viewModel.setOrClearFriendlyName(String.empty()) }
                then("Do nothing, just return") {
                    coVerify(exactly = 0) { settingsUseCase.setFriendlyName(any(), any()) }
                }
            }
            When("it's not null but it's the same as the current one") {
                runTest { viewModel.setOrClearFriendlyName(viewModel.device.friendlyName) }
                then("Do nothing, just return") {
                    coVerify(exactly = 0) { settingsUseCase.setFriendlyName(any(), any()) }
                }
            }
            When("it's not null and it's a new friendly name") {
                and("a usecase returning the result of setting a new friendly name") {
                    and("it's a failure") {
                        coMockEitherLeft(
                            { settingsUseCase.setFriendlyName(device.id, newFriendlyName) },
                            failure
                        )
                        runTest { viewModel.setOrClearFriendlyName(newFriendlyName) }
                        then("analytics should track the event's failure") {
                            testFriendlyNameAnalyticsEvent(0L, 1)
                            verify(exactly = 1) { analytics.trackEventFailure(any()) }
                        }
                        then("LiveData onError should post the respective generic error") {
                            viewModel.onError().value shouldBe UIError(REACH_OUT_MSG)
                        }
                    }
                    and("it's a success") {
                        coMockEitherRight(
                            { settingsUseCase.setFriendlyName(device.id, newFriendlyName) },
                            Unit
                        )
                        runTest { viewModel.setOrClearFriendlyName(newFriendlyName) }
                        then("analytics should track the event's success") {
                            testFriendlyNameAnalyticsEvent(1L, 1)
                        }
                        then("LiveData onEditNameChange posts the new friendly name") {
                            viewModel.onEditNameChange().value shouldBe newFriendlyName
                        }
                        then("we update the friendlyName in our device object") {
                            viewModel.device.friendlyName shouldBe newFriendlyName
                        }
                    }
                }
            }
            When("it's null") {
                and("the current device's friendly name is null/empty") {
                    viewModel.device.friendlyName = null
                    runTest { viewModel.setOrClearFriendlyName(null) }
                    then("Do nothing, just return") {
                        coVerify(exactly = 0) { settingsUseCase.clearFriendlyName(any()) }
                    }
                }
                and("the current's device friendly name is NOT empty") {
                    viewModel.device.friendlyName = newFriendlyName
                    and("a usecase returning the result of setting a new friendly name") {
                        and("it's a failure") {
                            coMockEitherLeft(
                                { settingsUseCase.clearFriendlyName(device.id) },
                                failure
                            )
                            runTest { viewModel.setOrClearFriendlyName(null) }
                            then("analytics should track the event's failure") {
                                testFriendlyNameAnalyticsEvent(0L, 2)
                                verify(exactly = 2) { analytics.trackEventFailure(any()) }
                            }
                            then("LiveData onError should post the respective generic error") {
                                viewModel.onError().value shouldBe UIError(REACH_OUT_MSG)
                            }
                        }
                        and("it's a success") {
                            coMockEitherRight(
                                { settingsUseCase.clearFriendlyName(device.id) },
                                Unit
                            )
                            runTest { viewModel.setOrClearFriendlyName(null) }
                            then("analytics should track the event's success") {
                                testFriendlyNameAnalyticsEvent(1L, 2)
                            }
                            then("LiveData onEditNameChange posts the new friendly name") {
                                viewModel.onEditNameChange().value shouldBe device.name
                            }
                            then("we update the friendlyName in our device object") {
                                viewModel.device.friendlyName shouldBe null
                            }
                        }
                    }
                }
            }
        }
    }

    context("Remove a device") {
        given("a usecase returning the result of the removal") {
            When("it's a failure") {
                and("it's an InvalidClaimId failure") {
                    coMockEitherLeft(
                        { settingsUseCase.removeDevice("label", device.id) },
                        invalidClaimIdFailure
                    )
                    runTest { viewModel.removeDevice() }
                    then("LiveData onError should post the respective Invalid ID error") {
                        viewModel.onError().value shouldBe UIError("invalidIdentifier")
                    }
                }
                and("it's any other failure") {
                    coMockEitherLeft({ settingsUseCase.removeDevice("label", device.id) }, failure)
                    runTest { viewModel.removeDevice() }
                    then("LiveData onError should post the respective generic error") {
                        viewModel.onError().value shouldBe UIError(REACH_OUT_MSG)
                    }
                }
                then("analytics should track the event's failure (2 failures so 2 more events)") {
                    verify(exactly = 4) { analytics.trackEventFailure(any()) }
                }
            }
            When("it's a success") {
                coMockEitherRight({ settingsUseCase.removeDevice("label", device.id) }, Unit)
                runTest { viewModel.removeDevice() }
                then("LiveData onDeviceRemoved should post a success with Unit value") {
                    viewModel.onDeviceRemoved().value shouldBe true
                }
            }
        }
    }

    context("Get text to share from DeviceInfo") {
        given("a DeviceInfo object") {
            then("get the sharing text") {
                viewModel.parseDeviceInfoToShare(uiDeviceInfoToShare) shouldBe sharingText
            }
        }
    }

    context("Get if a user is stakeholder or not") {
        given("some reward splits data") {
            When("user is stakeholder") {
                then("return true") {
                    viewModel.isStakeholder(stakeholderSplits) shouldBe true
                }
            }
            When("user is not stakeholder") {
                then("return false") {
                    viewModel.isStakeholder(nonStakeholderSplits) shouldBe false
                }
            }
        }
    }

    context("Get Device Photos") {
        given("a usecase returning the result of the photos") {
            When("it's a success") {
                coMockEitherRight({ photosUseCase.getDevicePhotos(device.id) }, listOf<String>())
                runTest { viewModel.getDevicePhotos() }
                then("LiveData onPhotos should post the List<String> created") {
                    viewModel.onPhotos().value shouldBe listOf<String>()
                }
            }
        }
    }

    context("Event onPhotosChanged has been triggered") {
        given("an argument `shouldDeleteAllPhotos` and the respective list of photos to delete") {
            When("it's true") {
                then("it should delete all the photos passed as arguments") {
                    viewModel.onPhotosChanged(true, stationPhotos)
                }
            }
            When("it's false") {
                then("get the updated device photos") {
                    coMockEitherRight(
                        { photosUseCase.getDevicePhotos(device.id) },
                        listOf("testUrl")
                    )
                    runTest { viewModel.getDevicePhotos() }
                    viewModel.onPhotos().value shouldBe listOf("testUrl")
                }
            }
        }
    }

    context("Get device photos upload IDs") {
        given("a device ID") {
            then("return the list of upload IDs") {
                viewModel.getDevicePhotoUploadIds() shouldBe uploadIds
            }
        }
    }

    context("Retry photo uploading") {
        given("a deviceId") {
            then("trigger the retrying of photo uploading") {
                viewModel.retryPhotoUpload()
                verify(exactly = 1) { photosUseCase.retryUpload(device.id) }
            }
        }
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
                    verify(exactly = 5) { analytics.trackEventFailure(any()) }
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

    context("Get if the user has accepted the uploading photos terms") {
        given("The usecase providing the GET / SET mechanisms") {
            When("We should get the user's accepted status") {
                and("user has not accepted the terms") {
                    every { photosUseCase.getAcceptedTerms() } returns false
                    then("return false") {
                        viewModel.getAcceptedPhotoTerms() shouldBe false
                    }
                }
                and("user has accepted the terms") {
                    every { photosUseCase.getAcceptedTerms() } returns true
                    then("return true") {
                        viewModel.getAcceptedPhotoTerms() shouldBe true
                    }
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
