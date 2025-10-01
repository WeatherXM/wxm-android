package com.weatherxm.ui.home.explorer

import com.weatherxm.data.models.Bundle
import com.weatherxm.data.models.Location
import com.weatherxm.ui.common.BundleName
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class UIModelsTest : BehaviorSpec({
    val searchResult = SearchResult(
        name = "name",
        Location.empty(),
        "address",
        Bundle("d1", "D1", "wifi", "WS1001", "WG1200", "hwClass"),
        "cellIndex",
        "stationId",
        DeviceRelation.OWNED
    )
    val nullSearchResult = SearchResult(null, null)

    val deviceOfSearchResult = UIDevice(
        id = "stationId",
        name = "name",
        cellIndex = "cellIndex",
        cellCenter = Location.empty(),
        relation = DeviceRelation.OWNED,
        bundleName = BundleName.d1,
        bundleTitle = "D1",
        connectivity = "wifi",
        wsModel = "WS1001",
        gwModel = "WG1200",
        hwClass = "hwClass",
        isActive = null,
        lastWeatherStationActivity = null,
        timezone = null,
        address = "address",
        isDeviceFromSearchResult = true,
        currentWeather = null,
        assignedFirmware = null,
        currentFirmware = null,
        claimedAt = null,
        friendlyName = null,
        label = null,
        location = null,
        hex7 = null,
        totalRewards = null,
        actualReward = null,
        qodScore = null,
        polReason = null,
        metricsTimestamp = null,
        cellDataQuality = null,
        hasLowBattery = null,
        hasLowGwBattery = null
    )

    val deviceOfNullSearchResult = UIDevice(
        id = "",
        name = "",
        cellIndex = "",
        cellCenter = null,
        relation = null,
        bundleName = null,
        bundleTitle = null,
        connectivity = null,
        wsModel = null,
        gwModel = null,
        hwClass = null,
        isActive = null,
        lastWeatherStationActivity = null,
        timezone = null,
        address = null,
        isDeviceFromSearchResult = true,
        currentWeather = null,
        assignedFirmware = null,
        currentFirmware = null,
        claimedAt = null,
        friendlyName = null,
        label = null,
        location = null,
        hex7 = null,
        totalRewards = null,
        actualReward = null,
        qodScore = null,
        polReason = null,
        metricsTimestamp = null,
        hasLowBattery = null,
        hasLowGwBattery = null,
        cellDataQuality = null
    )

    context("Get a UIDevice from a SearchResult") {
        given("a search result") {
            When("the SearchResult has NOT null data") {
                then("use the respective function to get the UIDevice") {
                    searchResult.toUIDevice() shouldBe deviceOfSearchResult
                }
            }
            When("the SearchResult has null data") {
                then("use the respective function to get the UIDevice") {
                    nullSearchResult.toUIDevice() shouldBe deviceOfNullSearchResult
                }
            }
        }
    }
})
