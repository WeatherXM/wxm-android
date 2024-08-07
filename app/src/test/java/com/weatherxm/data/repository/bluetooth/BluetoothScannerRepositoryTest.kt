package com.weatherxm.data.repository.bluetooth

import com.juul.kable.Advertisement
import com.weatherxm.data.datasource.bluetooth.BluetoothScannerDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow

class BluetoothScannerRepositoryTest : BehaviorSpec({
    val dataSource = mockk<BluetoothScannerDataSource>()
    val repository = BluetoothScannerRepositoryImpl(dataSource)

    val flow = mockk<Flow<Advertisement>>()

    beforeSpec {
        coEvery { dataSource.scan() } returns flow
    }

    context("Get the Scanning Advertisements Flow") {
        given("The data source providing the flow") {
            then("The repository should return that flow") {
                repository.scan() shouldBe flow
            }
        }
    }
})
