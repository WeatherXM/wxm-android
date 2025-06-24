package com.weatherxm.usecases

import android.icu.text.CompactDecimalFormat
import android.text.format.DateFormat
import com.github.mikephil.charting.data.Entry
import com.weatherxm.TestConfig.context
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.data.HOUR_FORMAT_24H
import com.weatherxm.data.models.Connectivity
import com.weatherxm.data.models.NetworkStatsContracts
import com.weatherxm.data.models.NetworkStatsCustomers
import com.weatherxm.data.models.NetworkStatsDune
import com.weatherxm.data.models.NetworkStatsGrowth
import com.weatherxm.data.models.NetworkStatsHealth
import com.weatherxm.data.models.NetworkStatsResponse
import com.weatherxm.data.models.NetworkStatsRewards
import com.weatherxm.data.models.NetworkStatsStation
import com.weatherxm.data.models.NetworkStatsStationDetails
import com.weatherxm.data.models.NetworkStatsTimeseries
import com.weatherxm.data.models.NetworkStatsToken
import com.weatherxm.data.models.NetworkStatsTokenMetrics
import com.weatherxm.data.models.NetworkStatsTotalAllocated
import com.weatherxm.data.models.NetworkStatsWeatherStations
import com.weatherxm.data.repository.StatsRepository
import com.weatherxm.ui.networkstats.NetworkStationStats
import com.weatherxm.util.DateTimeHelper.getFormattedDateAndTime
import com.weatherxm.util.NumberUtils
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.text.NumberFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class StatsUseCaseTest : KoinTest, BehaviorSpec({
    val repo = mockk<StatsRepository>()
    val usecase = StatsUseCaseImpl(repo, context)

    val firstDate = ZonedDateTime.of(2024, 6, 25, 2, 0, 0, 0, ZoneId.of("UTC"))
    val middleDate = ZonedDateTime.of(2024, 6, 26, 2, 0, 0, 0, ZoneId.of("UTC"))
    val lastDate = ZonedDateTime.of(2024, 6, 27, 2, 0, 0, 0, ZoneId.of("UTC"))
    val testNetworkStats = NetworkStatsResponse(
        health = NetworkStatsHealth(
            networkAvgQod = 80,
            activeStations = 15000,
            networkUptime = 98,
            health30DaysGraph = listOf(
                NetworkStatsTimeseries(firstDate, 0.0),
                NetworkStatsTimeseries(middleDate, 1000.0),
                NetworkStatsTimeseries(lastDate, 20000.0)
            ),
        ),
        growth = NetworkStatsGrowth(
            networkSize = 23000,
            networkScaleUp = 13,
            last30Days = 150,
            last30DaysGraph = listOf(
                NetworkStatsTimeseries(firstDate, 0.0),
                NetworkStatsTimeseries(middleDate, 1000.0),
                NetworkStatsTimeseries(lastDate, 20000.0)
            ),
        ),
        rewards = NetworkStatsRewards(
            total = 2500000,
            lastRun = 35000,
            last30Days = 150000,
            last30DaysGraph = listOf(
                NetworkStatsTimeseries(middleDate, 1000.0),
                NetworkStatsTimeseries(lastDate, 100000.0)
            ),
            lastTxHashUrl = "testTxHash",
            tokenMetrics = NetworkStatsTokenMetrics(
                totalAllocated = NetworkStatsTotalAllocated(
                    dune = NetworkStatsDune(
                        total = 15000,
                        claimed = 7500,
                        unclaimed = 7500,
                        duneUrl = "duneUrl"
                    ),
                    baseRewards = 100,
                    boostRewards = 50
                ),
                token = NetworkStatsToken(
                    totalSupply = 100000000,
                    circulatingSupply = 50000000
                )
            )
        ),
        weatherStations = NetworkStatsWeatherStations(
            NetworkStatsStation(
                100,
                listOf(
                    NetworkStatsStationDetails("test", Connectivity.wifi, "test", 50, 0.5),
                    NetworkStatsStationDetails("test", Connectivity.helium, "test", 50, 0.5)
                )
            ),
            NetworkStatsStation(
                100,
                listOf(
                    NetworkStatsStationDetails("test", Connectivity.wifi, "test", 60, 0.6),
                    NetworkStatsStationDetails("test", Connectivity.cellular, "test", 40, 0.4)
                )
            ),
            NetworkStatsStation(
                100,
                listOf(
                    NetworkStatsStationDetails("test", Connectivity.wifi, "test", 40, 0.4),
                    NetworkStatsStationDetails("test", Connectivity.helium, "test", 60, 0.6)
                )
            )
        ),
        contracts = NetworkStatsContracts("testTokenUrl", "testRewardsUrl"),
        customers = NetworkStatsCustomers(1000, 900),
        lastUpdated = lastDate
    )

    beforeSpec {
        startKoin {
            modules(module {
                single<DateTimeFormatter>(named(HOUR_FORMAT_24H)) {
                    DateTimeFormatter.ofPattern(HOUR_FORMAT_24H)
                }
                single<CompactDecimalFormat> {
                    mockk<CompactDecimalFormat>()
                }
                single<NumberFormat> {
                    NumberFormat.getInstance(Locale.US)
                }
            })
        }

        every { DateFormat.is24HourFormat(context) } returns true
        every { NumberUtils.compactNumber(any()) } returns "1"
    }

    context("Get Network Stats") {
        given("an API call that fetches the Network Stats") {
            When("the API call fails") {
                coMockEitherLeft({ repo.getNetworkStats() }, failure)
                then("a failure should be returned") {
                    usecase.getNetworkStats().isError()
                }
            }
            When("the API call returns some data") {
                coMockEitherRight({ repo.getNetworkStats() }, testNetworkStats)
                then("the correct transformation to the UI Model should take place") {
                    val response = usecase.getNetworkStats()
                    response.isRight() shouldBe true
                    response.onRight {
                        it.uptime shouldBe "98%"
                        it.netDataQualityScore shouldBe "80%"
                        it.healthActiveStations shouldBe "15,000"
                        it.uptimeEntries[0].equalTo(Entry(0F, 0F)) shouldBe true
                        it.uptimeEntries[1].equalTo(Entry(1F, 1000F)) shouldBe true
                        it.uptimeEntries[2].equalTo(Entry(2F, 20000F)) shouldBe true
                        it.uptimeStartDate shouldBe "Jun 25"
                        it.uptimeEndDate shouldBe "Jun 27"
                        it.netScaleUp shouldBe "13%"
                        it.netSize shouldBe "23,000"
                        it.netAddedInLast30Days shouldBe "150"
                        it.growthEntries[0].equalTo(Entry(0F, 0F)) shouldBe true
                        it.growthEntries[1].equalTo(Entry(1F, 1000F)) shouldBe true
                        it.growthEntries[2].equalTo(Entry(2F, 20000F)) shouldBe true
                        it.growthStartDate shouldBe "Jun 25"
                        it.growthEndDate shouldBe "Jun 27"
                        it.totalRewards shouldBe "1"
                        it.totalRewards30D shouldBe "1"
                        it.lastRewards shouldBe "1"
                        it.rewardsEntries[0].equalTo(Entry(0F, 1000F)) shouldBe true
                        it.rewardsEntries[1].equalTo(Entry(1F, 100000F)) shouldBe true
                        it.rewardsStartDate shouldBe "Jun 26"
                        it.rewardsEndDate shouldBe "Jun 27"
                        it.duneUrl shouldBe "duneUrl"
                        it.duneClaimed shouldBe 7500
                        it.duneUnclaimed shouldBe 7500
                        it.duneTotal shouldBe 15000
                        it.baseRewards shouldBe "1"
                        it.boostRewards shouldBe "1"
                        it.totalSupply shouldBe 100000000
                        it.circulatingSupply shouldBe 50000000
                        it.lastTxHashUrl shouldBe "testTxHash"
                        it.tokenUrl shouldBe "testTokenUrl"
                        it.rewardsUrl shouldBe "testRewardsUrl"
                        it.totalStations shouldBe "100"
                        it.totalStationStats shouldBe listOf(
                            NetworkStationStats("test", "test", 50.0, "50"),
                            NetworkStationStats("test", "test", 50.0, "50")
                        )
                        it.claimedStations shouldBe "100"
                        it.claimedStationStats shouldBe listOf(
                            NetworkStationStats("test", "test", 60.0, "60"),
                            NetworkStationStats("test", "test", 40.0, "40")
                        )
                        it.activeStations shouldBe "100"
                        it.activeStationStats shouldBe listOf(
                            NetworkStationStats("test", "test", 40.0, "40"),
                            NetworkStationStats("test", "test", 60.0, "60")
                        )
                        it.lastUpdated shouldBe
                            lastDate?.withZoneSameInstant(ZoneId.systemDefault())
                                .getFormattedDateAndTime(context)
                    }
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
