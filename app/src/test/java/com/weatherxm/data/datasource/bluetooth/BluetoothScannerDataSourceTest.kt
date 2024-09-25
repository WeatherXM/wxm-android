package com.weatherxm.data.datasource.bluetooth

import com.juul.kable.Advertisement
import com.weatherxm.data.bluetooth.BluetoothScanner
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow

class BluetoothScannerDataSourceTest : BehaviorSpec({
    val scanner = mockk<BluetoothScanner>()
    val datasource = BluetoothScannerDataSourceImpl(scanner)

    val flow = mockk<Flow<Advertisement>>()

    beforeSpec {
        coEvery { scanner.scan() } returns flow
    }

    context("Get the Scanning Advertisements Flow") {
        given("The scanner providing the flow") {
            then("return that flow") {
                datasource.scan() shouldBe flow
            }
        }
    }
})
