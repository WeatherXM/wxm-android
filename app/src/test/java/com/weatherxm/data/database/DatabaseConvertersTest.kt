package com.weatherxm.data.database

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date

class DatabaseConvertersTest : BehaviorSpec({
    val converters = DatabaseConverters()
    val testZonedDateTime = ZonedDateTime.of(
        2022,
        1,
        1,
        0,
        0,
        0,
        0,
        ZoneOffset.UTC
    )
    val testDate = Date.from(testZonedDateTime.toInstant())

    context("Convert to and from ZonedDateTime") {
        given("a ZonedDateTime") {
            then("return a String") {
                converters.fromZonedDateTime(testZonedDateTime) shouldBe "2022-01-01T00:00Z"
            }
        }
        given("a String") {
            then("return a ZonedDateTime") {
                converters.toZonedDateTime("2022-01-01T00:00Z") shouldBe testZonedDateTime
            }
        }
    }

    context("Convert to and from Date") {
        given("a Date") {
            then("return a String") {
                converters.fromDate(testDate) shouldBe testZonedDateTime.toInstant().toEpochMilli()
            }
        }
        given("a String") {
            then("return a ZonedDateTime") {
                converters.toDate(testZonedDateTime.toInstant().toEpochMilli()) shouldBe testDate
            }
        }
    }
})
