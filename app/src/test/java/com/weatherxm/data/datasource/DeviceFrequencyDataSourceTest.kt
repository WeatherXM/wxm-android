package com.weatherxm.data.datasource

import com.weatherxm.TestConfig.successUnitResponse
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.data.models.Frequency
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.DeviceFrequencyBody
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk

class DeviceFrequencyDataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val networkSource = DeviceFrequencyDataSourceImpl(apiService)

    val serialNumber = "serialNumber"
    val frequency = Frequency.EU868.name

    context("Set a Device's frequency") {
        When("Using the Network Source") {
            testNetworkCall(
                "Unit",
                Unit,
                successUnitResponse,
                mockFunction = {
                    apiService.setDeviceFrequency(DeviceFrequencyBody(serialNumber, frequency))
                },
                runFunction = { networkSource.setDeviceFrequency(serialNumber, frequency) }
            )
        }
    }
})
