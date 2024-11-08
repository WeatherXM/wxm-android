package com.weatherxm.usecases

import android.net.Uri
import com.weatherxm.TestConfig.context
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.OTAState
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.bluetooth.BluetoothUpdaterRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.Flow

class BluetoothUpdaterUseCaseTest : BehaviorSpec({
    val repository = mockk<BluetoothUpdaterRepository>()
    val deviceOtaRepository = mockk<DeviceOTARepository>()
    val usecase = BluetoothUpdaterUseCaseImpl(context, repository, deviceOtaRepository, dispatcher)

    val deviceId = "deviceId"
    val otaVersion = "1.0.0"
    val firmware = ByteArray(1)
    val flow = mockk<Flow<OTAState>>()
    val uri = mockk<Uri>()

    beforeSpec {
        every { context.cacheDir.path } returns "./"
        coEvery { repository.update(any()) } returns flow
        mockkStatic(Uri::class)
        every { Uri.fromFile(any()) } returns uri
        coJustRun { deviceOtaRepository.onUpdateSuccess(deviceId, otaVersion) }
    }

    context("Get the firmware for the device") {
        given("The repository providing the firmware") {
            and("The device ID") {
                When("Firmware is available") {
                    coMockEitherRight({ deviceOtaRepository.getFirmware(deviceId) }, firmware)
                    then("return the Uri which the firmware can be accessed from") {
                        usecase.downloadFirmwareAndGetFileURI(deviceId).isSuccess(uri)
                    }
                }
                When("Firmware is NOT available") {
                    coMockEitherLeft({ deviceOtaRepository.getFirmware(deviceId) }, failure)
                    then("return null") {
                        usecase.downloadFirmwareAndGetFileURI(deviceId).isError()
                    }
                }
            }
        }
    }

    context("Get the Update OTAState Flow") {
        given("The repository providing the flow") {
            then("The usecase should return that flow") {
                usecase.update(mockk()) shouldBe flow
            }
        }
    }

    context("Trigger the onUpdateSuccess") {
        given("The repository accepting this trigger") {
            then("The usecase should trigger the repository") {
                usecase.onUpdateSuccess(deviceId, otaVersion)
                coVerify(exactly = 1) { deviceOtaRepository.onUpdateSuccess(deviceId, otaVersion) }
            }
        }
    }
})
