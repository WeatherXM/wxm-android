package ui.usecases

import android.content.Context
import android.icu.text.CompactDecimalFormat
import android.icu.text.NumberFormat
import android.text.format.DateFormat
import arrow.core.Either
import com.github.mikephil.charting.data.Entry
import com.weatherxm.data.Connectivity
import com.weatherxm.data.HOUR_FORMAT_24H
import com.weatherxm.data.NetworkError
import com.weatherxm.data.NetworkStatsContracts
import com.weatherxm.data.NetworkStatsCustomers
import com.weatherxm.data.NetworkStatsResponse
import com.weatherxm.data.NetworkStatsStation
import com.weatherxm.data.NetworkStatsStationDetails
import com.weatherxm.data.NetworkStatsTimeseries
import com.weatherxm.data.NetworkStatsTokens
import com.weatherxm.data.NetworkStatsWeatherStations
import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.StatsRepository
import com.weatherxm.ui.common.MainnetInfo
import com.weatherxm.ui.networkstats.NetworkStationStats
import com.weatherxm.usecases.StatsUseCaseImpl
import com.weatherxm.util.DateTimeHelper.getFormattedDateAndTime
import com.weatherxm.util.NumberUtils
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class StatsUseCaseTest : KoinTest, BehaviorSpec({
    val repo = mockk<StatsRepository>()
    val appConfigRepository = mockk<AppConfigRepository>()
    val context = mockk<Context>()
    val usecase = StatsUseCaseImpl(repo, appConfigRepository, context)

    startKoin {
        modules(
            module {
                single<DateTimeFormatter>(named(HOUR_FORMAT_24H)) {
                    DateTimeFormatter.ofPattern(HOUR_FORMAT_24H)
                }
                single<CompactDecimalFormat> {
                    mockk<CompactDecimalFormat>()
                }
                single<NumberFormat> {
                    mockk<NumberFormat>()
                }
            }
        )
    }

    val testMessage = "testMessage"
    val testUrl = "testUrl"
    val firstDate = ZonedDateTime.of(2024, 6, 25, 2, 0, 0, 0, ZoneId.of("UTC"))
    val middleDate = ZonedDateTime.of(2024, 6, 26, 2, 0, 0, 0, ZoneId.of("UTC"))
    val lastDate = ZonedDateTime.of(2024, 6, 27, 2, 0, 0, 0, ZoneId.of("UTC"))
    val testNetworkStats = NetworkStatsResponse(
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
                    NetworkStatsStationDetails("test", Connectivity.helium, "test", 40, 0.4)
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
        dataDays = listOf(
            NetworkStatsTimeseries(firstDate, 0.0),
            NetworkStatsTimeseries(middleDate, 1000.0),
            NetworkStatsTimeseries(lastDate, 20000.0)
        ),
        tokens = NetworkStatsTokens(
            100000000,
            5000000,
            listOf(
                NetworkStatsTimeseries(middleDate, 1000.0),
                NetworkStatsTimeseries(lastDate, 100000.0)
            ),
            500135.0,
            5000000,
            "testTxHash"
        ),
        contracts = NetworkStatsContracts("testTokenUrl", "testRewardsUrl"),
        customers = NetworkStatsCustomers(1000, 900),
        lastUpdated = lastDate
    )

    beforeSpec {
        every { appConfigRepository.isMainnetEnabled() } returns true
        every { appConfigRepository.getMainnetUrl() } returns testUrl
        every { appConfigRepository.getMainnetMessage() } returns testMessage

        mockkStatic(DateFormat::class)
        every { DateFormat.is24HourFormat(context) } returns true

        every { NumberUtils.compactNumber(any()) } returns "1"
        every { NumberUtils.formatNumber(any()) } returns "1"
    }

    context("Get mainnet-related info") {
        given("that mainnet is enabled") {
            then("return the correct value") {
                usecase.isMainnetEnabled() shouldBe true
            }
            and("get the correct mainnet info") {
                usecase.getMainnetInfo() shouldBe MainnetInfo(testMessage, testUrl)
            }
        }
    }
    context("Get Network Stats") {
        given("an API call that fetches the Network Stats") {
            When("the API call fails") {
                coEvery {
                    repo.getNetworkStats()
                } returns Either.Left(NetworkError.NoConnectionError())
                then("a failure should be returned") {
                    usecase.getNetworkStats()
                        .isLeft { it is NetworkError.NoConnectionError } shouldBe true
                }
            }
            When("the API call returns some data") {
                coEvery { repo.getNetworkStats() } returns Either.Right(testNetworkStats)
                then("the correct transformation to the UI Model should take place") {
                    val response = usecase.getNetworkStats()
                    response.isRight() shouldBe true
                    response.onRight {
                        it.totalDataDays shouldBe "1"
                        it.totalDataDays30D shouldBe "1"
                        it.lastDataDays shouldBe "1"
                        it.dataDaysEntries[0].equalTo(Entry(0F, 0F)) shouldBe true
                        it.dataDaysEntries[1].equalTo(Entry(1F, 1000F)) shouldBe true
                        it.dataDaysEntries[2].equalTo(Entry(2F, 20000F)) shouldBe true
                        it.dataDaysStartDate shouldBe "Jun 25"
                        it.dataDaysEndDate shouldBe "Jun 27"
                        it.totalRewards shouldBe "1"
                        it.totalRewards30D shouldBe "1"
                        it.lastRewards shouldBe "1"
                        it.rewardsEntries[0].equalTo(Entry(0F, 1000F)) shouldBe true
                        it.rewardsEntries[1].equalTo(Entry(1F, 100000F)) shouldBe true
                        it.rewardsStartDate shouldBe "Jun 26"
                        it.rewardsEndDate shouldBe "Jun 27"
                        it.rewardsAvgMonthly shouldBe "1"
                        it.totalSupply shouldBe 100000000
                        it.circulatingSupply shouldBe 5000000
                        it.lastTxHashUrl shouldBe "testTxHash"
                        it.tokenUrl shouldBe "testTokenUrl"
                        it.rewardsUrl shouldBe "testRewardsUrl"
                        it.totalStations shouldBe "1"
                        it.totalStationStats shouldBe listOf(
                            NetworkStationStats("test", "test", 50.0, "1"),
                            NetworkStationStats("test", "test", 50.0, "1")
                        )
                        it.claimedStations shouldBe "1"
                        it.claimedStationStats shouldBe listOf(
                            NetworkStationStats("test", "test", 60.0, "1"),
                            NetworkStationStats("test", "test", 40.0, "1")
                        )
                        it.activeStations shouldBe "1"
                        it.activeStationStats shouldBe listOf(
                            NetworkStationStats("test", "test", 40.0, "1"),
                            NetworkStationStats("test", "test", 60.0, "1")
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
