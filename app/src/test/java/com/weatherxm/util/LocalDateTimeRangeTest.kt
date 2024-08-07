package com.weatherxm.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class LocalDateTimeRangeTest : BehaviorSpec({
    val startYesterday = LocalDateTime.now().minusDays(1)
    val endNow = LocalDateTime.now()
    val range = LocalDateTimeRange(startYesterday, endNow)
    val validPrintedRange = "LocalDateTimeRange[${startYesterday}...${endNow}]"

    given("A LocalDateTimeRange") {
        then("Getting all using map function") {
            range.all() shouldBe range.map { it }
        }
        then("The start should be yesterday") {
            range.start() shouldBe startYesterday
        }
        then("The end should be now") {
            range.end() shouldBe endNow
        }
        then("Printing it should be $validPrintedRange") {
            range.toString() shouldBe validPrintedRange
        }
        then("The hashcode should be the same") {
            range.hashCode() shouldBe validPrintedRange.hashCode()
        }
        and("Equality should be true") {
            (range == range) shouldBe true
            and("A different LocalDateTime is used to rangeTo using it") {
                (startYesterday.rangeTo(endNow) == range) shouldBe true
            }
        }
        When("An iterator gets created") {
            val iterator = LocalDateTimeIterator(startYesterday, endNow)
            val overridenIterator = range.iterator()
            then("Iterator should be equal to the overriden one") {
                (iterator == overridenIterator) shouldBe true
            }
            then("It should have next values") {
                iterator.hasNext() shouldBe true
            }
            then("The first value should be yesterday") {
                iterator.next() shouldBe startYesterday
            }
            then("The second value should be yesterday +1 hour") {
                iterator.next() shouldBe startYesterday.plusHours(1)
            }
            then("Iterating out of bounds should produce NoSuchElementException") {
                repeat(24) {
                    try {
                        iterator.next()
                    } catch (e: NoSuchElementException) {
                        e.message shouldBe "Reached the end of this LocalDateTimeRange"
                    }
                }
                iterator.hasNext() shouldBe false
            }
        }
    }
})
