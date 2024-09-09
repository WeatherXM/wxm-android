package com.weatherxm.usecases

import android.content.Context
import androidx.compose.ui.util.fastForEachIndexed
import arrow.core.Either
import com.github.mikephil.charting.data.Entry
import com.weatherxm.R
import com.weatherxm.data.BoostCode
import com.weatherxm.data.BoostReward
import com.weatherxm.data.BoostRewardDetails
import com.weatherxm.data.Failure
import com.weatherxm.data.RewardDetails
import com.weatherxm.data.RewardsCode
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.data.repository.RewardsRepositoryImpl
import com.weatherxm.ui.common.BoostDetailInfo
import com.weatherxm.ui.common.DeviceTotalRewardsBoost
import com.weatherxm.ui.common.DeviceTotalRewardsDetails
import com.weatherxm.ui.common.DevicesRewardsByRange
import com.weatherxm.ui.common.LineChartData
import com.weatherxm.ui.common.RewardTimelineType
import com.weatherxm.ui.common.TimelineReward
import com.weatherxm.ui.common.UIBoost
import com.weatherxm.ui.common.UIRewardsTimeline
import com.weatherxm.ui.common.empty
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.DateTimeHelper.getFormattedMonthDate
import com.weatherxm.util.NumberUtils.formatNumber
import com.weatherxm.util.Rewards.formatTokens
import com.weatherxm.util.getName
import com.weatherxm.util.getShortName
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

interface RewardsUseCase {
    suspend fun getRewardsTimeline(
        deviceId: String,
        page: Int?
    ): Either<Failure, UIRewardsTimeline>

    suspend fun getRewardDetails(
        deviceId: String,
        date: ZonedDateTime
    ): Either<Failure, RewardDetails>

    suspend fun getBoostReward(
        deviceId: String,
        boostReward: BoostReward
    ): Either<Failure, UIBoost>

    suspend fun getDevicesRewardsByRange(
        mode: RewardsRepositoryImpl.Companion.RewardsSummaryMode
    ): Either<Failure, DevicesRewardsByRange>

    suspend fun getDeviceRewardsByRange(
        deviceId: String,
        mode: RewardsRepositoryImpl.Companion.RewardsSummaryMode
    ): Either<Failure, DeviceTotalRewardsDetails>
}

class RewardsUseCaseImpl(
    private val repository: RewardsRepository,
    private val context: Context
) : RewardsUseCase {
    override suspend fun getRewardsTimeline(
        deviceId: String,
        page: Int?
    ): Either<Failure, UIRewardsTimeline> {
        return repository.getRewardsTimeline(
            deviceId = deviceId,
            page = page
        ).map {
            if (it.data.isEmpty()) {
                UIRewardsTimeline(listOf(), reachedTotal = true)
            } else {
                UIRewardsTimeline(
                    it.data.filter { tx ->
                        // Keep only transactions that have a reward for this device
                        tx.baseReward != null
                    }.map { reward ->
                        TimelineReward(RewardTimelineType.DATA, reward)
                    },
                    it.hasNextPage
                )
            }
        }
    }

    override suspend fun getRewardDetails(
        deviceId: String,
        date: ZonedDateTime
    ): Either<Failure, RewardDetails> {
        return repository.getRewardDetails(deviceId, date)
    }

    override suspend fun getBoostReward(
        deviceId: String,
        boostReward: BoostReward
    ): Either<Failure, UIBoost> {
        val boostCode = boostReward.code ?: String.empty()
        return repository.getBoostReward(deviceId, boostCode).map {
            val actualReward = boostReward.actualReward ?: 0F
            val maxReward = boostReward.maxReward ?: 0F
            val boostDetails = mutableListOf<BoostDetailInfo>()

            it.details?.stationHours?.let { amount ->
                boostDetails.add(
                    BoostDetailInfo(
                        context.getString(R.string.rewardable_station_hours), formatNumber(amount)
                    )
                )
            }

            it.details?.maxDailyReward?.let { amount ->
                boostDetails.add(
                    BoostDetailInfo(
                        context.getString(R.string.daily_tokens_to_be_rewarded),
                        context.getString(R.string.wxm_amount, formatTokens(amount))
                    )
                )
            }

            it.details?.maxTotalReward?.let { amount ->
                boostDetails.add(
                    BoostDetailInfo(
                        context.getString(R.string.total_tokens_to_be_rewarded),
                        context.getString(R.string.wxm_amount, formatTokens(amount))
                    )
                )
            }

            if (it.details?.boostStartDate != null && it.details.boostStopDate != null) {
                val boostStartDate = it.details.boostStartDate.getFormattedDate(true)
                val boostStopDate = it.details.boostStopDate.getFormattedDate(true)
                boostDetails.add(
                    BoostDetailInfo(
                        context.getString(R.string.boost_period), "$boostStartDate - $boostStopDate"
                    )
                )
            }

            UIBoost(
                it.metadata?.title ?: String.empty(),
                formatTokens(actualReward),
                boostReward.rewardScore,
                formatTokens(maxReward - actualReward),
                getBoostDesc(boostCode, it.details),
                it.metadata?.about ?: String.empty(),
                it.metadata?.docUrl ?: String.empty(),
                it.metadata?.imgUrl ?: String.empty(),
                boostDetails
            )
        }
    }

    override suspend fun getDevicesRewardsByRange(
        mode: RewardsRepositoryImpl.Companion.RewardsSummaryMode
    ): Either<Failure, DevicesRewardsByRange> {
        return repository.getDevicesRewardsByRange(mode).map { rewards ->
            val xLabels = mutableListOf<String>()
            val entries = mutableListOf<Entry>()
            val datesChartTooltip = mutableListOf<String>()

            rewards.data?.fastForEachIndexed { i, timeseries ->
                /**
                 * 7D = Show 3-letter days - e.g. Mon, Tue, Wed,
                 * 1M = DD/MM or MM/DD based on Locale - e.g. 25/01 or 01/25
                 * 1Y = Show 3-letter months as the design - e.g. Sep, Oct, Nov
                 */
                when (mode) {
                    RewardsRepositoryImpl.Companion.RewardsSummaryMode.WEEK -> {
                        datesChartTooltip.add(context.getString(timeseries.ts.dayOfWeek.getName()))
                        xLabels.add(context.getString(timeseries.ts.dayOfWeek.getShortName()))
                    }
                    RewardsRepositoryImpl.Companion.RewardsSummaryMode.MONTH -> {
                        datesChartTooltip.add(timeseries.ts.getFormattedDate())
                        xLabels.add(timeseries.ts.getFormattedMonthDate())
                    }
                    RewardsRepositoryImpl.Companion.RewardsSummaryMode.YEAR -> {
                        datesChartTooltip.add(
                            timeseries.ts.month.getDisplayName(TextStyle.FULL, Locale.US)
                        )
                        xLabels.add(timeseries.ts.month.getDisplayName(TextStyle.SHORT, Locale.US))
                    }
                }
                entries.add(Entry(i.toFloat(), timeseries.totalRewards ?: 0F))
            }

            DevicesRewardsByRange(
                rewards.total, mode, datesChartTooltip, LineChartData(xLabels, entries)
            )
        }
    }

    override suspend fun getDeviceRewardsByRange(
        deviceId: String,
        mode: RewardsRepositoryImpl.Companion.RewardsSummaryMode
    ): Either<Failure, DeviceTotalRewardsDetails> {
        return repository.getDeviceRewardsByRange(deviceId, mode).map { summary ->
            val xLabels = mutableListOf<String>()
            val baseEntries = mutableListOf<Entry>()
            val betaEntries = mutableListOf<Entry>()
            val otherEntries = mutableListOf<Entry>()
            val datesChartTooltip = mutableListOf<String>()

            summary.data?.fastForEachIndexed { i, timeseries ->
                val counter = i.toFloat()
                val emptyEntry = Entry(counter, Float.NaN)
                /**
                 * 7D = Show 3-letter days - e.g. Mon, Tue, Wed,
                 * 1M = DD/MM or MM/DD based on Locale - e.g. 25/01 or 01/25
                 * 1Y = Show 3-letter months as the design - e.g. Sep, Oct, Nov
                 */
                when (mode) {
                    RewardsRepositoryImpl.Companion.RewardsSummaryMode.WEEK -> {
                        datesChartTooltip.add(context.getString(timeseries.ts.dayOfWeek.getName()))
                        xLabels.add(context.getString(timeseries.ts.dayOfWeek.getShortName()))
                    }
                    RewardsRepositoryImpl.Companion.RewardsSummaryMode.MONTH -> {
                        datesChartTooltip.add(timeseries.ts.getFormattedDate())
                        xLabels.add(timeseries.ts.getFormattedMonthDate())
                    }
                    RewardsRepositoryImpl.Companion.RewardsSummaryMode.YEAR -> {
                        datesChartTooltip.add(
                            timeseries.ts.month.getDisplayName(TextStyle.FULL, Locale.US)
                        )
                        xLabels.add(timeseries.ts.month.getDisplayName(TextStyle.SHORT, Locale.US))
                    }
                }

                val baseCode = RewardsCode.base_reward.name
                val betaCode = RewardsCode.beta_rewards.name


                var sum = 0F
                var baseSum = 0F
                var betaSum = 0F
                var othersSum = 0F

                timeseries.rewards?.forEach {
                    if (it.code == baseCode) {
                        baseSum += it.value ?: 0F
                    }
                    if (it.code == betaCode) {
                        betaSum += it.value ?: 0F
                    }
                    if (it.code != baseCode && it.code != betaCode) {
                        othersSum += it.value ?: 0F
                    }
                    sum += it.value ?: 0F
                }
                if(baseSum == 0F) {
                    baseEntries.add(emptyEntry)
                } else {
                    baseEntries.add(Entry(counter, baseSum))
                }
                if(betaSum == 0F) {
                    betaEntries.add(emptyEntry)
                } else {
                    betaEntries.add(Entry(counter, betaSum + baseSum))
                }
                if(othersSum == 0F) {
                    otherEntries.add(emptyEntry)
                } else {
                    otherEntries.add(Entry(counter, othersSum + betaSum + baseSum))
                }
//                baseEntries.add(Entry(counter, baseSum))
//                betaEntries.add(Entry(counter, betaSum + baseSum))
//                otherEntries.add(Entry(counter, othersSum + betaSum + baseSum))


//                timeseries.rewards?.firstOrNull {
//                    it.code == baseCode
//                }?.value?.let {
//                    baseEntries.add(Entry(counter, it))
//                } ?: baseEntries.add(emptyEntry)
//
//                timeseries.rewards?.firstOrNull {
//                    it.code == betaCode
//                }?.value?.let {
//                    betaEntries.add(Entry(counter, it))
//                } ?: betaEntries.add(emptyEntry)
//
//                timeseries.rewards?.sumOf {
//                    if (it.code != baseCode && it.code != betaCode) {
//                        it.value?.toDouble() ?: 0.0
//                    } else {
//                        0.0
//                    }
//                }.also {
//                    if (it == null || it == 0.0) {
//                        otherEntries.add(emptyEntry)
//                    } else {
//                        otherEntries.add(Entry(counter, it.toFloat()))
//                    }
//                }
            }

            DeviceTotalRewardsDetails(
                summary.total,
                mode,
                summary.details?.map {
                    DeviceTotalRewardsBoost(
                        it.code,
                        it.completedPercentage?.toInt(),
                        it.totalRewards,
                        it.currentRewards,
                        it.boostPeriodStart,
                        it.boostPeriodEnd
                    )
                },
                datesChartTooltip,
                LineChartData(xLabels, baseEntries),
                LineChartData(xLabels, betaEntries),
                LineChartData(xLabels, otherEntries),
                false
            )
        }
    }

    private fun getBoostDesc(
        boostCode: String,
        details: BoostRewardDetails?
    ): String {
        return try {
            if (BoostCode.valueOf(boostCode) == BoostCode.beta_rewards) {
                val participationStartDate =
                    details?.participationStartDate.getFormattedDate(true)
                val participationStopDate =
                    details?.participationStopDate.getFormattedDate(true)
                context.getString(
                    R.string.boost_details_beta_desc, participationStartDate, participationStopDate
                )
            } else {
                String.empty()
            }
        } catch (e: IllegalArgumentException) {
            Timber.e("Unsupported Boost Code: $boostCode")
            String.empty()
        }
    }
}
