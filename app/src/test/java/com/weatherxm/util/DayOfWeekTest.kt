package com.weatherxm.util

import com.weatherxm.R
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecGivenContainerScope
import io.kotest.matchers.shouldBe
import java.time.DayOfWeek

class DayOfWeekTest : BehaviorSpec({
    suspend fun BehaviorSpecGivenContainerScope.testDayOfWeek(
        testDay: DayOfWeek,
        nameExpected: Int,
        shortNameExpected: Int,
        firstLetterExpected: Int
    ) {
        then("get name") {
            testDay.getName() shouldBe nameExpected
        }
        and("get short name") {
            testDay.getShortName() shouldBe shortNameExpected
        }
        and("get first letter") {
            testDay.getFirstLetter() shouldBe firstLetterExpected
        }
    }

    given("DayOfWeek.MONDAY") {
        this.testDayOfWeek(
            DayOfWeek.MONDAY,
            R.string.monday,
            R.string.mon,
            R.string.mon_first_letter
        )
    }

    given("DayOfWeek.TUESDAY") {
        this.testDayOfWeek(
            DayOfWeek.TUESDAY,
            R.string.tuesday,
            R.string.tue,
            R.string.tue_first_letter
        )
    }

    given("DayOfWeek.WEDNESDAY") {
        this.testDayOfWeek(
            DayOfWeek.WEDNESDAY,
            R.string.wednesday,
            R.string.wed,
            R.string.wed_first_letter
        )
    }

    given("DayOfWeek.THURSDAY") {
        this.testDayOfWeek(
            DayOfWeek.THURSDAY,
            R.string.thursday,
            R.string.thu,
            R.string.thu_first_letter
        )
    }

    given("DayOfWeek.FRIDAY") {
        this.testDayOfWeek(
            DayOfWeek.FRIDAY,
            R.string.friday,
            R.string.fri,
            R.string.fri_first_letter
        )
    }

    given("DayOfWeek.SATURDAY") {
        this.testDayOfWeek(
            DayOfWeek.SATURDAY,
            R.string.saturday,
            R.string.sat,
            R.string.sat_first_letter
        )
    }

    given("DayOfWeek.SUNDAY") {
        this.testDayOfWeek(
            DayOfWeek.SUNDAY,
            R.string.sunday,
            R.string.sun,
            R.string.sun_first_letter
        )
    }
})
