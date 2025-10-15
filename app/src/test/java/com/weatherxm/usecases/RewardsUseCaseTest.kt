package com.weatherxm.usecases

import android.icu.text.CompactDecimalFormat
import android.text.format.DateFormat
import com.github.mikephil.charting.data.Entry
import com.weatherxm.R
import com.weatherxm.TestConfig.context
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isEqual
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.DATE_FORMAT_MONTH_DAY
import com.weatherxm.data.DECIMAL_FORMAT_TOKENS
import com.weatherxm.data.THOUSANDS_GROUPING_SIZE
import com.weatherxm.data.models.BoostReward
import com.weatherxm.data.models.BoostRewardDetails
import com.weatherxm.data.models.BoostRewardMetadata
import com.weatherxm.data.models.BoostRewardResponse
import com.weatherxm.data.models.DeviceRewardsSummary
import com.weatherxm.data.models.DeviceRewardsSummaryData
import com.weatherxm.data.models.DeviceRewardsSummaryDataReward
import com.weatherxm.data.models.DeviceRewardsSummaryDetails
import com.weatherxm.data.models.DevicesRewards
import com.weatherxm.data.models.DevicesRewardsData
import com.weatherxm.data.models.Reward
import com.weatherxm.data.models.RewardDetails
import com.weatherxm.data.models.RewardsCode
import com.weatherxm.data.models.RewardsTimeline
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.data.repository.RewardsRepositoryImpl.Companion.RewardsSummaryMode
import com.weatherxm.ui.common.BoostDetailInfo
import com.weatherxm.ui.common.DeviceTotalRewardsBoost
import com.weatherxm.ui.common.DeviceTotalRewardsDetails
import com.weatherxm.ui.common.DevicesRewardsByRange
import com.weatherxm.ui.common.LineChartData
import com.weatherxm.ui.common.RewardTimelineType
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.TimelineReward
import com.weatherxm.ui.common.UIBoost
import com.weatherxm.ui.common.UIRewardsTimeline
import com.weatherxm.ui.common.empty
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.NumberUtils
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class RewardsUseCaseTest : BehaviorSpec({
    val repository = mockk<RewardsRepository>()
    val usecase = RewardsUseCaseImpl(repository, context)

    val deviceId = "deviceId"
    val page = 0
    val timestamp = ZonedDateTime.now()
    val formattedDate = timestamp.getFormattedDate(true)
    val emptyReward = Reward(timestamp, null, null, null, null, null)
    val validReward = Reward(timestamp, 10F, 0F, 10F, 100, null)

    val emptyTimeline = RewardsTimeline(emptyList(), 1, false)
    val emptyUITimeline = UIRewardsTimeline(emptyList())

    val timeline = RewardsTimeline(listOf(emptyReward, validReward), 1, false)
    val uiTimeline =
        UIRewardsTimeline(listOf(TimelineReward(RewardTimelineType.DATA, validReward)), false)

    val rewardDetails = mockk<RewardDetails>()

    val boostCode = "beta_rewards"
    val boostReward = BoostReward(boostCode, null, null, null, null, 10F, 100, 10F)
    val emptyBoostReward = BoostReward(null, null, null, null, null, null, null, null)
    val boostRewardsResponse = BoostRewardResponse(
        boostCode,
        BoostRewardMetadata("title", "about", "imgUrl", "docUrl", "about"),
        BoostRewardDetails(10, 10F, 10F, timestamp, timestamp, timestamp, timestamp)
    )
    val emptyBoostRewardResponse = BoostRewardResponse(null, null, null)
    val uiBoost = UIBoost(
        title = "title",
        actualReward = "10.00",
        boostScore = 100,
        lostRewards = "0.00",
        boostDesc = "Boost details description",
        about = "about",
        docUrl = "docUrl",
        imgUrl = "imgUrl",
        details = listOf(
            BoostDetailInfo("Rewardable station-hours", "10"),
            BoostDetailInfo("Daily tokens to be rewarded (max)", "10 \$WXM"),
            BoostDetailInfo("Total tokens to be rewarded (max)", "10 \$WXM"),
            BoostDetailInfo("Boost Period", "$formattedDate - $formattedDate")
        )
    )
    val emptyUiBoost =
        UIBoost(
            String.empty(),
            "0.00",
            null,
            "0.00",
            String.empty(),
            String.empty(),
            String.empty(),
            String.empty(),
            emptyList()
        )

    val customTimestamp = ZonedDateTime.of(2024, 1, 1, 8, 0, 0, 0, ZoneId.of("UTC"))
    val devicesRewards =
        DevicesRewards(100F, mutableListOf(DevicesRewardsData(customTimestamp, 100F)))
    val devicesRewardsByRangeWeek = DevicesRewardsByRange(
        100F,
        RewardsSummaryMode.WEEK,
        listOf("Monday"),
        LineChartData(mutableListOf("Mon"), mutableListOf(Entry(0F, 100F)))
    )
    val devicesRewardsByRangeMonth = DevicesRewardsByRange(
        100F,
        RewardsSummaryMode.MONTH,
        listOf("Jan 1"),
        LineChartData(mutableListOf("1/1"), mutableListOf(Entry(0F, 100F)))
    )

    val deviceRewardsSummary = DeviceRewardsSummary(
        140F,
        listOf(
            DeviceRewardsSummaryData(
                customTimestamp,
                listOf(
                    DeviceRewardsSummaryDataReward("base", RewardsCode.base_reward.name, 40F),
                    DeviceRewardsSummaryDataReward("boost", RewardsCode.beta_rewards.name, 40F),
                    DeviceRewardsSummaryDataReward("correction", RewardsCode.correction.name, 20F),
                    DeviceRewardsSummaryDataReward("trov2", RewardsCode.trov2.name, 20F),
                    DeviceRewardsSummaryDataReward("boost", "other", 20F)
                )
            )
        ),
        listOf(
            DeviceRewardsSummaryDetails(
                "boost",
                RewardsCode.beta_rewards.name,
                40F,
                81F,
                customTimestamp,
                customTimestamp,
                49.3F
            )
        )
    )
    val deviceTotalRewardsDetails = DeviceTotalRewardsDetails(
        140F,
        RewardsSummaryMode.WEEK,
        listOf(
            DeviceTotalRewardsBoost(
                RewardsCode.beta_rewards.name,
                49,
                81F,
                40F,
                customTimestamp,
                customTimestamp
            )
        ),
        listOf(140F),
        listOf("Monday"),
        LineChartData(mutableListOf("Mon"), mutableListOf(Entry(0F, 40F))),
        LineChartData(mutableListOf("Mon"), mutableListOf(Entry(0F, 80F))),
        LineChartData(mutableListOf("Mon"), mutableListOf(Entry(0F, 100F))),
        LineChartData(mutableListOf("Mon"), mutableListOf(Entry(0F, 120F))),
        LineChartData(mutableListOf("Mon"), mutableListOf(Entry(0F, 140F))),
        Status.SUCCESS
    )

    beforeSpec {
        startKoin {
            modules(
                module {
                    single<CompactDecimalFormat> {
                        mockk<CompactDecimalFormat>()
                    }
                    single<NumberFormat> {
                        NumberFormat.getInstance(Locale.getDefault())
                    }
                    single<DateTimeFormatter>(named(DATE_FORMAT_MONTH_DAY)) {
                        val usersLocaleDateFormat =
                            DateFormat.getBestDateTimePattern(
                                Locale.getDefault(),
                                DATE_FORMAT_MONTH_DAY
                            )
                        DateTimeFormatter.ofPattern(usersLocaleDateFormat)
                    }
                    single<DecimalFormat>(named(DECIMAL_FORMAT_TOKENS)) {
                        DecimalFormat(DECIMAL_FORMAT_TOKENS).apply {
                            roundingMode = RoundingMode.HALF_UP
                            groupingSize = THOUSANDS_GROUPING_SIZE
                            isGroupingUsed = true
                        }
                    }
                }
            )
        }
        every { NumberUtils.compactNumber(any()) } returns "10"
        every { context.getString(R.string.monday) } returns "Monday"
        every { context.getString(R.string.mon) } returns "Mon"
    }

    context("Get timeline of rewards") {
        given("A repository providing the timeline") {
            When("it's a success") {
                and("Data are empty") {
                    then("return an empty timeline") {
                        coMockEitherRight(
                            { repository.getRewardsTimeline(deviceId, page) },
                            emptyTimeline
                        )
                        usecase.getRewardsTimeline(deviceId, page).isSuccess(emptyUITimeline)
                    }
                }
                and("Data are not empty") {
                    then("return the UIRewardsTimeline") {
                        coMockEitherRight(
                            { repository.getRewardsTimeline(deviceId, page) },
                            timeline
                        )
                        usecase.getRewardsTimeline(deviceId, page).isSuccess(uiTimeline)
                    }
                }
            }
            When("it's a failure") {
                then("return the failure") {
                    coMockEitherLeft({ repository.getRewardsTimeline(deviceId, page) }, failure)
                    usecase.getRewardsTimeline(deviceId, page).isError()
                }
            }
        }
    }

    context("Get reward details") {
        given("A repository providing the reward details") {
            When("it's a success") {
                then("return the RewardDetails") {
                    coMockEitherRight(
                        { repository.getRewardDetails(deviceId, timestamp) },
                        rewardDetails
                    )
                    usecase.getRewardDetails(deviceId, timestamp).isSuccess(rewardDetails)
                }
            }
            When("it's a failure") {
                then("return the failure") {
                    coMockEitherLeft({ repository.getRewardDetails(deviceId, timestamp) }, failure)
                    usecase.getRewardDetails(deviceId, timestamp).isError()
                }
            }
        }
    }

    context("Get boost reward") {
        given("A repository providing the boost reward") {
            When("it's a success") {
                and("it's a known BoostReward") {
                    coMockEitherRight(
                        { repository.getBoostReward(deviceId, boostCode) },
                        boostRewardsResponse
                    )
                    then("return the UIBoost") {
                        usecase.getBoostReward(deviceId, boostReward).isSuccess(uiBoost)
                    }
                }
                and("it's an empty BoostReward") {
                    coMockEitherRight(
                        { repository.getBoostReward(deviceId, String.empty()) },
                        emptyBoostRewardResponse
                    )
                    then("return an empty UIBoost") {
                        usecase.getBoostReward(deviceId, emptyBoostReward).isSuccess(emptyUiBoost)
                    }
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ repository.getBoostReward(deviceId, boostCode) }, failure)
                then("return the failure") {
                    usecase.getBoostReward(deviceId, boostReward).isError()
                }
            }
        }
    }

    context("Get rewards summary for all devices by range") {
        given("A repository providing the rewards summary") {
            When("it's a success") {
                and("mode is WEEK") {
                    coMockEitherRight(
                        { repository.getDevicesRewardsByRange(RewardsSummaryMode.WEEK) },
                        devicesRewards
                    )
                    then("return the DevicesRewardsByRange for WEEK mode") {
                        usecase.getDevicesRewardsByRange(RewardsSummaryMode.WEEK).getOrNull().also {
                            it?.total shouldBe devicesRewardsByRangeWeek.total
                            it?.mode shouldBe devicesRewardsByRangeWeek.mode
                            it?.datesChartTooltip shouldBe
                                devicesRewardsByRangeWeek.datesChartTooltip
                            it?.lineChartData?.isEqual(devicesRewardsByRangeWeek.lineChartData)
                        }
                    }
                }
                and("mode is MONTH") {
                    coMockEitherRight(
                        { repository.getDevicesRewardsByRange(RewardsSummaryMode.MONTH) },
                        devicesRewards
                    )
                    then("return the DevicesRewardsByRange for MONTH mode") {
                        usecase.getDevicesRewardsByRange(RewardsSummaryMode.MONTH).getOrNull()
                            .also {
                                it?.total shouldBe devicesRewardsByRangeMonth.total
                                it?.mode shouldBe devicesRewardsByRangeMonth.mode
                                it?.datesChartTooltip shouldBe
                                    devicesRewardsByRangeMonth.datesChartTooltip
                                it?.lineChartData?.isEqual(devicesRewardsByRangeMonth.lineChartData)
                            }
                    }
                }
            }
            When("it's a failure") {
                coMockEitherLeft(
                    { repository.getDevicesRewardsByRange(RewardsSummaryMode.WEEK) },
                    failure
                )
                then("return the failure") {
                    usecase.getDevicesRewardsByRange(RewardsSummaryMode.WEEK).isError()
                }
            }
        }
    }

    context("Get rewards summary for a device by range") {
        given("A repository providing the device's rewards summary") {
            When("it's a success") {
                and("mode is WEEK") {
                    coMockEitherRight(
                        { repository.getDeviceRewardsByRange(deviceId, RewardsSummaryMode.WEEK) },
                        deviceRewardsSummary
                    )
                    then("return the DeviceTotalRewardsDetails for WEEK mode") {
                        usecase.getDeviceRewardsByRange(deviceId, RewardsSummaryMode.WEEK)
                            .getOrNull()
                            .also {
                                it?.total shouldBe deviceTotalRewardsDetails.total
                                it?.mode shouldBe deviceTotalRewardsDetails.mode
                                it?.boosts shouldBe deviceTotalRewardsDetails.boosts
                                it?.totals shouldBe deviceTotalRewardsDetails.totals
                                it?.datesChartTooltip shouldBe
                                    deviceTotalRewardsDetails.datesChartTooltip
                                it?.baseChartData?.isEqual(deviceTotalRewardsDetails.baseChartData)
                                it?.betaChartData?.isEqual(deviceTotalRewardsDetails.betaChartData)
                                it?.otherChartData?.isEqual(
                                    deviceTotalRewardsDetails.otherChartData
                                )
                                it?.status shouldBe deviceTotalRewardsDetails.status
                            }
                    }
                }
            }
            When("it's a failure") {
                coMockEitherLeft(
                    { repository.getDeviceRewardsByRange(deviceId, RewardsSummaryMode.WEEK) },
                    failure
                )
                then("return the failure") {
                    usecase.getDeviceRewardsByRange(deviceId, RewardsSummaryMode.WEEK).isError()
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
