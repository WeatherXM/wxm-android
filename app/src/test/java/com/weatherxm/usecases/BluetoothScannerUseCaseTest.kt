package com.weatherxm.usecases

import com.juul.kable.Advertisement
import com.weatherxm.data.repository.bluetooth.BluetoothScannerRepository
import com.weatherxm.ui.common.ScannedDevice
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow

class BluetoothScannerUseCaseTest : BehaviorSpec({
    val repository = mockk<BluetoothScannerRepository>()
    val usecase = BluetoothScannerUseCaseImpl(repository)

    val flow = mockk<Flow<Advertisement>>()

    beforeSpec {
        coEvery { repository.scan() } returns flow
    }

    context("Get the Scanning Advertisements Flow") {
        given("The repository providing the flow") {
            then("The usecase should return that flow") {
                (usecase.scan() is Flow<ScannedDevice>) shouldBe true
            }
        }
    }
})
