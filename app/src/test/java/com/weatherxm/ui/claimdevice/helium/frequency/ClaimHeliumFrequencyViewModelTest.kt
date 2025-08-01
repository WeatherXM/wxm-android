package com.weatherxm.ui.claimdevice.helium.frequency

import com.weatherxm.R
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.resources
import com.weatherxm.data.models.CountryAndFrequencies
import com.weatherxm.data.models.Frequency
import com.weatherxm.data.models.Location
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.FrequencyState
import com.weatherxm.usecases.ClaimDeviceUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class ClaimHeliumFrequencyViewModelTest : BehaviorSpec({
    val usecase = mockk<ClaimDeviceUseCase>()
    lateinit var viewModel: ClaimHeliumFrequencyViewModel

    val location = Location.empty()
    val countryAndFrequencies = CountryAndFrequencies(
        "GR",
        Frequency.EU868,
        listOf(Frequency.US915, Frequency.AU915)
    )
    val recommendedLabel = "Recommended for GR"
    val frequencyState = FrequencyState("GR", listOf("EU868 ($recommendedLabel)", "US915", "AU915"))

    listener(InstantExecutorListener())

    beforeSpec {
        coEvery { usecase.getCountryAndFrequencies(location) } returns countryAndFrequencies
        every {
            resources.getString(R.string.recommended_frequency_for, "GR")
        } returns recommendedLabel

        viewModel = ClaimHeliumFrequencyViewModel(usecase, resources, dispatcher)
    }

    context("Get Country and Frequencies") {
        given("a usecase returning the country and frequencies") {
            runTest { viewModel.getCountryAndFrequencies(location) }
            then("init the correct variables") {
                viewModel.getFrequency(0) shouldBe countryAndFrequencies.recommendedFrequency
                viewModel.getFrequency(1) shouldBe countryAndFrequencies.otherFrequencies[0]
                viewModel.getFrequency(2) shouldBe countryAndFrequencies.otherFrequencies[1]
            }
            then("LiveData onFrequencyState should post the correct FrequencyState") {
                viewModel.onFrequencyState().value shouldBe frequencyState
            }
        }
    }

    afterSpec {
    }
})
