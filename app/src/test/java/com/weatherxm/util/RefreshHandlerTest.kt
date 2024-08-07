package com.weatherxm.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

class RefreshHandlerTest : BehaviorSpec({
    val refreshHandler = RefreshHandler(refreshIntervalMillis = 10) // Short interval for testing

    given("the refresh flow") {
        val emissions = refreshHandler.flow()
            .take(2)
            .toList()
        then("it should emit twice") {
            emissions shouldBe listOf(Unit, Unit)
        }
    }
})
