package com.weatherxm.ui.managesubscription

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.FlowPreview

@OptIn(FlowPreview::class)
class ManageSubscriptionViewModelTest : BehaviorSpec({
    val viewModel = ManageSubscriptionViewModel()

    val offerToken = "offerToken"

    context("Get the initial value of the offer token") {
        given("The ViewModel's GET function") {
            then("return it") {
                viewModel.getOfferToken() shouldBe null
            }
        }
    }

    context("Set a new value of the offer token") {
        given("The ViewModel's SET function") {
            then("ensure that it's SET correctly") {
                viewModel.setOfferToken(offerToken)
                viewModel.getOfferToken() shouldBe offerToken
            }
        }
    }
})
