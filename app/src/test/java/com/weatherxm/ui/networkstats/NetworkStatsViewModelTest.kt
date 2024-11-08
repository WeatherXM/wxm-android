package com.weatherxm.ui.networkstats

import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.usecases.StatsUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class NetworkStatsViewModelTest : BehaviorSpec({
    val usecase = mockk<StatsUseCase>()
    val viewModel = NetworkStatsViewModel(usecase, dispatcher)
    val networkStats = mockk<NetworkStats>()

    listener(InstantExecutorListener())

    context("Get network stats") {
        given("A use case providing the network stats") {
            When("it's a success") {
                coMockEitherRight({ usecase.getNetworkStats() }, networkStats)
                then("LiveData posts a success") {
                    runTest { viewModel.getNetworkStats() }
                    viewModel.onNetworkStats().isSuccess(networkStats)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ usecase.getNetworkStats() }, failure)
                runTest { viewModel.getNetworkStats() }
                then("LiveData posts an error with a default message") {
                    viewModel.onNetworkStats().isError(REACH_OUT_MSG)
                }
            }
        }
    }
})
