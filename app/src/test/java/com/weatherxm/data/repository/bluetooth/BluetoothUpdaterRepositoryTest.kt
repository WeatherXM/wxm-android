package com.weatherxm.data.repository.bluetooth

import com.weatherxm.data.models.OTAState
import com.weatherxm.data.datasource.bluetooth.BluetoothUpdaterDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow

class BluetoothUpdaterRepositoryTest : BehaviorSpec({
    val dataSource = mockk<BluetoothUpdaterDataSource>()
    val repository = BluetoothUpdaterRepositoryImpl(dataSource)

    val flow = mockk<Flow<OTAState>>()

    beforeSpec {
        coEvery { dataSource.update(any()) } returns flow
    }

    context("Get the Update OTAState Flow") {
        given("The data source providing the flow") {
            then("The repository should return that flow") {
                repository.update(mockk()) shouldBe flow
            }
        }
    }
})
