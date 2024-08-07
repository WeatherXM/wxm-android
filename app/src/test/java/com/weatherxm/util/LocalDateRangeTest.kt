package com.weatherxm.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.ZoneOffset

class LocalDateRangeTest : BehaviorSpec({
    val yesterday = LocalDate.now().minusDays(1)
    val now = LocalDate.now()
    val tomorrow = LocalDate.now().plusDays(1)
    val invalidDate = LocalDate.of(300000000, 12, 31)

    val range = LocalDateRange(yesterday, now)
    val validPrintedRange = "LocalDateRange[${yesterday}...${now}]"

    fun LocalDate.testRelativeComparisons(
        expectedYesterday: Boolean,
        expectedToday: Boolean,
        expectedTomorrow: Boolean
    ) {
        isYesterday() shouldBe expectedYesterday
        isToday() shouldBe expectedToday
        isTomorrow() shouldBe expectedTomorrow
    }

    given("A LocalDateRange") {
        then("Getting all using map function") {
            range.all() shouldBe range.map { it }
        }
        then("The start should be yesterday") {
            range.start() shouldBe yesterday
        }
        then("The end should be now") {
            range.end() shouldBe now
        }
        then("Printing it should be $validPrintedRange") {
            range.toString() shouldBe validPrintedRange
        }
        then("The hashcode should be the same") {
            range.hashCode() shouldBe validPrintedRange.hashCode()
        }
        and("Equality should be true") {
            (range == range) shouldBe true
            and("A different LocalDate is used to rangeTo using it") {
                (yesterday.rangeTo(now) == range) shouldBe true
            }
        }
        When("An iterator gets created") {
            val iterator = LocalDateIterator(yesterday, now)
            val overridenIterator = range.iterator()
            then("Iterator should be equal to the overriden one") {
                (iterator == overridenIterator) shouldBe true
            }
            then("It should have next values") {
                iterator.hasNext() shouldBe true
            }
            then("The first value should be yesterday") {
                iterator.next() shouldBe yesterday
            }
            then("The second value should be today") {
                iterator.next() shouldBe yesterday.plusDays(1)
            }
            then("Iterating out of bounds should produce NoSuchElementException") {
                try {
                    iterator.next()
                } catch (e: NoSuchElementException) {
                    e.message shouldBe "Reached the end of this LocalDateRange"
                } finally {
                    iterator.hasNext() shouldBe false
                }
            }
        }
    }

    given("Some LocalDates") {
        then("Relative comparisons should work") {
            yesterday.testRelativeComparisons(
                expectedYesterday = true,
                expectedToday = false,
                expectedTomorrow = false
            )
            now.testRelativeComparisons(
                expectedYesterday = false,
                expectedToday = true,
                expectedTomorrow = false
            )
            tomorrow.testRelativeComparisons(
                expectedYesterday = false,
                expectedToday = false,
                expectedTomorrow = true
            )
        }
        and("Getting if it's the same year should work") {
            now.isSameYear() shouldBe true
        }
        and("Now to epoch millis should work") {
            now.toUTCEpochMillis() shouldBe
                now.atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
        }
        and("Invalid LocalDate to epoch millis should return null") {
            invalidDate.toUTCEpochMillis() shouldBe null
        }
    }
})
