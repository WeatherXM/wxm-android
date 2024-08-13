package com.weatherxm.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime

class ZonedDateTimeTest : BehaviorSpec({
    val today = ZonedDateTime.now()
    val tomorrow = today.plusDays(1)

    given("A ZonedDateTime") {
        When("is today") {
            then("return true") {
                today.isToday() shouldBe true
            }
            and("Get ISO Date") {
                today.toISODate() shouldBe today.toLocalDate().toString()
            }
            and("Compare with different ZonedDateTime if they are same day & hour") {
                today.isSameDayAndHour(today) shouldBe true
                and("But with different hour than now") {
                    today.isSameDayAndHour(today.plusHours(1)) shouldBe false
                }
            }
        }
        When("is not today") {
            then("return false") {
                tomorrow.isToday() shouldBe false
            }
            and("Compare with different ZonedDateTime if they are same day & hour") {
                today.isSameDayAndHour(tomorrow) shouldBe false
            }
        }
    }
})
