package com.weatherxm.usecases

import android.content.Context
import androidx.compose.ui.util.fastForEachIndexed
import arrow.core.Either
import com.github.mikephil.charting.data.Entry
import com.weatherxm.R
import com.weatherxm.data.models.BoostCode
import com.weatherxm.data.models.BoostReward
import com.weatherxm.data.models.BoostRewardDetails
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.RewardDetails
import com.weatherxm.data.models.RewardsCode
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.data.repository.RewardsRepositoryImpl
import com.weatherxm.ui.common.BoostDetailInfo
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
import com.weatherxm.util.DateTimeHelper.getFormattedMonthDate
import com.weatherxm.util.NumberUtils.formatNumber
import com.weatherxm.util.NumberUtils.formatTokens
import com.weatherxm.util.getName
import com.weatherxm.util.getShortName
import timber.log.Timber
import java.time.ZonedDateTime

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
                UIRewardsTimeline(listOf())
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
                        context.getString(R.string.rewardable_station_hours),
                        formatNumber(amount)
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
                        context.getString(R.string.boost_period),
                        "$boostStartDate - $boostStopDate"
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
                datesChartTooltip.add(getTooltipDate(timeseries.ts, mode))
                xLabels.add(getXAxisLabel(timeseries.ts, mode))
                entries.add(Entry(i.toFloat(), timeseries.totalRewards ?: 0F))
            }

            DevicesRewardsByRange(
                rewards.total, mode, datesChartTooltip, LineChartData(xLabels, entries)
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override suspend fun getDeviceRewardsByRange(
        deviceId: String,
        mode: RewardsRepositoryImpl.Companion.RewardsSummaryMode
    ): Either<Failure, DeviceTotalRewardsDetails> {
        return repository.getDeviceRewardsByRange(deviceId, mode).map { summary ->
            val xLabels = mutableListOf<String>()
            val baseEntries = mutableListOf<Entry>()
            val betaEntries = mutableListOf<Entry>()
            val correctionEntries = mutableListOf<Entry>()
            val rolloutsEntries = mutableListOf<Entry>()
            val cellBountiesEntries = mutableListOf<Entry>()
            val otherEntries = mutableListOf<Entry>()
            val totals = mutableListOf<Float>()
            val datesChartTooltip = mutableListOf<String>()

            summary.data?.fastForEachIndexed { counter, timeseries ->
                datesChartTooltip.add(getTooltipDate(timeseries.ts, mode))
                xLabels.add(getXAxisLabel(timeseries.ts, mode))

                val baseCode = RewardsCode.base_reward.name
                val betaCode = RewardsCode.beta_rewards.name
                val correctionCode = RewardsCode.correction.name
                val rolloutsCode = RewardsCode.trov2.name
                val cellBountyCode = "cell-bounty"
                var sum = 0F
                var baseSum = 0F
                var baseFound = false
                var betaSum = 0F
                var betaFound = false
                var othersSum = 0F
                var othersFound = false
                var correctionSum = 0F
                var correctionFound = false
                var rolloutsSum = 0F
                var rolloutsFound = false
                var cellBountiesSum = 0F
                var cellBountiesFound = false

                /**
                 * In order for the "chart with filled layers" to work properly, we need to add
                 * each layer atop the others. So in our case that we have
                 * 1. base -> beta -> correction -> rollouts -> others
                 * 2. beta entries should be the sum of base and beta
                 * --- (so that the layer is above base)
                 * 3. correction entries should be the sum of beta and correction
                 * --- (so that the layer is above beta)
                 * etc for the others
                 */
                timeseries.rewards?.forEach {
                    val isBase = it.code == baseCode
                    val isBeta = it.code == betaCode
                    val isRollouts = it.code == rolloutsCode
                    val isCellBounty = it.code.startsWith(cellBountyCode)
                    val isCorrection = it.code.startsWith(correctionCode)

                    if (isBase) {
                        baseSum += it.value
                        baseFound = true
                    }
                    if (isBeta) {
                        betaSum += it.value
                        betaFound = true
                    }
                    if (isRollouts) {
                        rolloutsSum += it.value
                        rolloutsFound = true
                    }
                    if (isCorrection) {
                        correctionSum += it.value
                        correctionFound = true
                    }
                    if (isCellBounty) {
                        cellBountiesSum += it.value
                        cellBountiesFound = true
                    }
                    @Suppress("ComplexCondition")
                    if (!isBase && !isBeta && !isCorrection && !isRollouts && !isCellBounty) {
                        othersSum += it.value
                        othersFound = true
                    }
                    sum += it.value
                }
                totals.add(sum)

                baseEntries.createNewEntry(
                    x = counter,
                    yIfNotFound = Float.NaN,
                    yIfFound = baseSum,
                    isFound = baseFound,
                    sum = baseSum
                )
                betaEntries.createNewEntry(
                    x = counter,
                    yIfNotFound = Float.NaN,
                    yIfFound = betaSum + baseSum,
                    isFound = betaFound,
                    sum = betaSum
                )
                correctionEntries.createNewEntry(
                    x = counter,
                    yIfNotFound = -1F,
                    yIfFound = correctionSum + betaSum + baseSum,
                    isFound = correctionFound,
                    sum = correctionSum
                )
                rolloutsEntries.createNewEntry(
                    x = counter,
                    yIfNotFound = -1F,
                    yIfFound = rolloutsSum + betaSum + baseSum + correctionSum,
                    isFound = rolloutsFound,
                    sum = rolloutsSum
                )
                cellBountiesEntries.createNewEntry(
                    x = counter,
                    yIfNotFound = -1F,
                    yIfFound = cellBountiesSum + rolloutsSum + betaSum + baseSum + correctionSum,
                    isFound = cellBountiesFound,
                    sum = cellBountiesSum
                )
                otherEntries.createNewEntry(
                    x = counter,
                    yIfNotFound = Float.NaN,
                    yIfFound = othersSum + cellBountiesSum + correctionSum + betaSum + baseSum +
                        rolloutsSum,
                    isFound = othersFound,
                    sum = othersSum
                )
            }

            DeviceTotalRewardsDetails(
                summary.total,
                mode,
                summary.details?.map { it.toDeviceTotalRewardsBoost() } ?: mutableListOf(),
                totals,
                datesChartTooltip,
                LineChartData(xLabels, baseEntries),
                LineChartData(xLabels, betaEntries),
                LineChartData(xLabels, correctionEntries),
                LineChartData(xLabels, rolloutsEntries),
                LineChartData(xLabels, cellBountiesEntries),
                LineChartData(xLabels, otherEntries),
                Status.SUCCESS
            )
        }
    }

    private fun MutableList<Entry>.createNewEntry(
        x: Int,
        yIfNotFound: Float,
        yIfFound: Float,
        isFound: Boolean,
        sum: Float
    ) {
        add(
            if (!isFound) {
                Entry(x.toFloat(), yIfNotFound)
            } else {
                if (sum == 0F) {
                    Entry(x.toFloat(), 0F)
                } else {
                    Entry(x.toFloat(), yIfFound)
                }
            }
        )
    }

    private fun getTooltipDate(
        timestamp: ZonedDateTime,
        mode: RewardsRepositoryImpl.Companion.RewardsSummaryMode
    ): String {
        /**
         * 7D = Show full days - e.g. Monday, Tuesday, Wednesday,
         * 1M = Show short month name with the month day - e.g. Jan 1, Feb 21, Mar 30,
         */
        return when (mode) {
            RewardsRepositoryImpl.Companion.RewardsSummaryMode.WEEK -> {
                context.getString(timestamp.dayOfWeek.getName())
            }
            RewardsRepositoryImpl.Companion.RewardsSummaryMode.MONTH -> {
                timestamp.getFormattedDate()
            }
        }
    }

    private fun getXAxisLabel(
        timestamp: ZonedDateTime,
        mode: RewardsRepositoryImpl.Companion.RewardsSummaryMode
    ): String {
        /**
         * 7D = Show 3-letter days - e.g. Mon, Tue, Wed,
         * 1M = DD/MM or MM/DD based on Locale - e.g. 25/01 or 01/25
         */
        return when (mode) {
            RewardsRepositoryImpl.Companion.RewardsSummaryMode.WEEK -> {
                context.getString(timestamp.dayOfWeek.getShortName())
            }
            RewardsRepositoryImpl.Companion.RewardsSummaryMode.MONTH -> {
                timestamp.getFormattedMonthDate()
            }
        }
    }

    private fun getBoostDesc(
        boostCode: String,
        details: BoostRewardDetails?
    ): String {
        val isCodeSupported = try {
            BoostCode.valueOf(boostCode) == BoostCode.beta_rewards
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Unsupported Boost Code: $boostCode")
            false
        }

        return if (isCodeSupported) {
            context.getString(
                R.string.boost_details_beta_desc,
                details?.participationStartDate.getFormattedDate(true),
                details?.participationStopDate.getFormattedDate(true)
            )
        } else {
            String.empty()
        }
    }
}
