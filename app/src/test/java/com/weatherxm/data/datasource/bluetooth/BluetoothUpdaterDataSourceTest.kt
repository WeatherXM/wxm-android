package com.weatherxm.data.datasource.bluetooth

import com.weatherxm.data.bluetooth.BluetoothUpdater
import com.weatherxm.data.models.OTAState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow

class BluetoothUpdaterDataSourceTest : BehaviorSpec({
    val updater = mockk<BluetoothUpdater>()
    val datasource = BluetoothUpdaterDataSourceImpl(updater)

    val flow = mockk<Flow<OTAState>>()

    beforeSpec {
        coEvery { updater.update(any()) } returns flow
    }

    context("Get the Update OTAState Flow") {
        given("The data source providing the flow") {
            then("return that flow") {
                datasource.update(mockk()) shouldBe flow
            }
        }
    }
})
