package com.weatherxm.data.datasource

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.data.models.BoostCode
import com.weatherxm.data.models.BoostRewardResponse
import com.weatherxm.data.models.DeviceRewardsSummary
import com.weatherxm.data.models.DevicesRewards
import com.weatherxm.data.models.RewardDetails
import com.weatherxm.data.models.Rewards
import com.weatherxm.data.models.RewardsTimeline
import com.weatherxm.data.models.WalletRewards
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ErrorResponse
import com.weatherxm.data.repository.RewardsRepositoryImpl
import com.weatherxm.data.repository.RewardsRepositoryImpl.Companion.TIMELINE_MINUS_MONTHS_TO_FETCH
import com.weatherxm.util.toISODate
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import java.time.ZoneId
import java.time.ZonedDateTime

class RewardsDataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val datasource = RewardsDataSourceImpl(apiService)

    val deviceId = "deviceId"
    val walletAddress = "walletAddress"
    val page = 0
    val timezone = ZoneId.of("UTC").toString()
    val fromDate = ZonedDateTime.now().minusMonths(TIMELINE_MINUS_MONTHS_TO_FETCH).toISODate()
    val mode = RewardsRepositoryImpl.Companion.RewardsSummaryMode.WEEK.name
    val boostCode = BoostCode.beta_rewards.name

    val rewardsTimeline = mockk<RewardsTimeline>()
    val timelineResponse = NetworkResponse.Success<RewardsTimeline, ErrorResponse>(
        rewardsTimeline, retrofitResponse(rewardsTimeline)
    )

    val rewards = mockk<Rewards>()
    val rewardsResponse =
        NetworkResponse.Success<Rewards, ErrorResponse>(rewards, retrofitResponse(rewards))

    val rewardDetails = mockk<RewardDetails>()
    val rewardDetailsResponse = NetworkResponse.Success<RewardDetails, ErrorResponse>(
        rewardDetails, retrofitResponse(rewardDetails)
    )

    val boostReward = mockk<BoostRewardResponse>()
    val boostRewardResponse = NetworkResponse.Success<BoostRewardResponse, ErrorResponse>(
        boostReward, retrofitResponse(boostReward)
    )

    val walletRewards = mockk<WalletRewards>()
    val walletRewardsResponse = NetworkResponse.Success<WalletRewards, ErrorResponse>(
        walletRewards, retrofitResponse(walletRewards)
    )

    val devicesRewardsByRange = mockk<DevicesRewards>()
    val devicesRewardsByRangeResponse = NetworkResponse.Success<DevicesRewards, ErrorResponse>(
        devicesRewardsByRange, retrofitResponse(devicesRewardsByRange)
    )

    val deviceRewardsSummary = mockk<DeviceRewardsSummary>()
    val deviceRewardsSummaryResponse = NetworkResponse.Success<DeviceRewardsSummary, ErrorResponse>(
        deviceRewardsSummary, retrofitResponse(deviceRewardsSummary)
    )

    context("Get rewards timeline") {
        When("Using the Network Source") {
            testNetworkCall(
                "rewards timeline",
                rewardsTimeline,
                timelineResponse,
                mockFunction = {
                    apiService.getRewardsTimeline(
                        deviceId, page, timezone = timezone, fromDate = fromDate
                    )
                },
                runFunction = {
                    datasource.getRewardsTimeline(
                        deviceId, page, timezone = timezone, fromDate = fromDate
                    )
                }
            )
        }
    }

    context("Get rewards") {
        When("Using the Network Source") {
            testNetworkCall(
                "rewards",
                rewards,
                rewardsResponse,
                mockFunction = { apiService.getRewards(deviceId) },
                runFunction = { datasource.getRewards(deviceId) }
            )
        }
    }

    context("Get reward details for a date") {
        When("Using the Network Source") {
            testNetworkCall(
                "reward details",
                rewardDetails,
                rewardDetailsResponse,
                mockFunction = { apiService.getRewardDetails(deviceId, fromDate) },
                runFunction = { datasource.getRewardDetails(deviceId, fromDate) }
            )
        }
    }

    context("Get boost reward") {
        When("Using the Network Source") {
            testNetworkCall(
                "boost reward",
                boostReward,
                boostRewardResponse,
                mockFunction = { apiService.getBoostReward(deviceId, boostCode) },
                runFunction = { datasource.getBoostReward(deviceId, boostCode) }
            )
        }
    }

    context("Get wallet rewards") {
        When("Using the Network Source") {
            testNetworkCall(
                "wallet rewards",
                walletRewards,
                walletRewardsResponse,
                mockFunction = { apiService.getWalletRewards(walletAddress) },
                runFunction = { datasource.getWalletRewards(walletAddress) }
            )
        }
    }

    context("Get devices rewards by range") {
        When("Using the Network Source") {
            testNetworkCall(
                "devices rewards by range",
                devicesRewardsByRange,
                devicesRewardsByRangeResponse,
                mockFunction = { apiService.getDevicesRewardsByRange(mode) },
                runFunction = { datasource.getDevicesRewardsByRange(mode) }
            )
        }
    }

    context("Get device rewards summary by range") {
        When("Using the Network Source") {
            testNetworkCall(
                "device rewards summary by range",
                deviceRewardsSummary,
                deviceRewardsSummaryResponse,
                mockFunction = { apiService.getDeviceRewardsByRange(deviceId, mode) },
                runFunction = { datasource.getDeviceRewardsByRange(deviceId, mode) }
            )
        }
    }
})
