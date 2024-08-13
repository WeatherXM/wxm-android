package com.weatherxm.util

import android.text.format.DateFormat
import com.weatherxm.R
import com.weatherxm.TestConfig.context
import com.weatherxm.data.DATE_FORMAT_FULL
import com.weatherxm.data.DATE_FORMAT_MONTH_DAY
import com.weatherxm.data.DATE_FORMAT_MONTH_SHORT
import com.weatherxm.data.HOUR_FORMAT_12H_FULL
import com.weatherxm.data.HOUR_FORMAT_12H_HOUR_ONLY
import com.weatherxm.data.HOUR_FORMAT_24H
import com.weatherxm.ui.common.empty
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.DateTimeHelper.getFormattedDateAndTime
import com.weatherxm.util.DateTimeHelper.getFormattedTime
import com.weatherxm.util.DateTimeHelper.getRelativeDayAndMonthDay
import com.weatherxm.util.DateTimeHelper.getRelativeDayAndShort
import com.weatherxm.util.DateTimeHelper.getRelativeDayOrFull
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.DateTimeHelper.timestampToLocalDate
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class DateTimeHelperTest : BehaviorSpec({
    val localDateTime = LocalDateTime.of(2024, 5, 30, 14, 0)
    val zonedDateTime = ZonedDateTime.of(2024, 5, 30, 14, 0, 0, 0, ZoneId.of("UTC"))
    val nowZoned = ZonedDateTime.now()
    val nowRelativeMessage = "Just Now"
    val todayLocalDate = LocalDate.now()
    val yesterdayLocalDate = LocalDate.now().minusDays(1)
    val tomorrowLocalDate = LocalDate.now().plusDays(1)
    val customLocalDate = LocalDate.of(2024, 5, 30)
    val timestamp = 1717077600000L

    beforeSpec {
        mockkStatic(DateFormat::class)
        every {
            DateFormat.getBestDateTimePattern(
                Locale.getDefault(),
                DATE_FORMAT_MONTH_DAY
            )
        } returns "d/M"

        startKoin {
            modules(
                module {
                    single<DateTimeFormatter>(named(HOUR_FORMAT_24H)) {
                        DateTimeFormatter.ofPattern(HOUR_FORMAT_24H)
                    }
                    single<DateTimeFormatter>(named(HOUR_FORMAT_12H_FULL)) {
                        DateTimeFormatter.ofPattern(HOUR_FORMAT_12H_FULL)
                    }
                    single<DateTimeFormatter>(named(HOUR_FORMAT_12H_HOUR_ONLY)) {
                        DateTimeFormatter.ofPattern(HOUR_FORMAT_12H_HOUR_ONLY)
                    }
                    single<DateTimeFormatter>(named(DATE_FORMAT_FULL)) {
                        DateTimeFormatter.ofPattern(DATE_FORMAT_FULL, Locale.US)
                    }
                    single<DateTimeFormatter>(named(DATE_FORMAT_MONTH_DAY)) {
                        val usersLocaleDateFormat =
                            DateFormat.getBestDateTimePattern(
                                Locale.getDefault(),
                                DATE_FORMAT_MONTH_DAY
                            )
                        DateTimeFormatter.ofPattern(usersLocaleDateFormat)
                    }
                    single<DateTimeFormatter>(named(DATE_FORMAT_MONTH_SHORT)) {
                        DateTimeFormatter.ofPattern(DATE_FORMAT_MONTH_SHORT, Locale.US)
                    }
                }
            )
        }

        every { context.getString(R.string.today) } returns "Today"
        every { context.getString(R.string.yesterday) } returns "Yesterday"
        every { context.getString(R.string.tomorrow) } returns "Tomorrow"
        every { context.getString(R.string.thursday) } returns "Thursday"
        every { context.getString(R.string.thu) } returns "Thu"
    }

    given("a LocalDateTime") {
        When("user uses 24-hour format") {
            every { DateFormat.is24HourFormat(context) } returns true
            then("it should return the formatted time in 24-hour format") {
                localDateTime.getFormattedTime(context) shouldBe "14:00"
            }
        }
        When("user uses 12-hour format") {
            every { DateFormat.is24HourFormat(context) } returns false
            When("showMinutes12HourFormat = true") {
                then("it should return the formatted time in 12-hour format with minutes") {
                    localDateTime.getFormattedTime(context, true) shouldBe "2:00 PM"
                }
            }
            When("showMinutes12HourFormat = false") {
                then("it should return the formatted time in 12-hour format without minutes") {
                    localDateTime.getFormattedTime(context, false) shouldBe "2 PM"
                }
            }
        }
    }

    given("A ZonedDateTime") {
        When("It's null") {
            then("Get formatted date and time") {
                null.getFormattedDate() shouldBe String.empty()
            }
        }
        When("It's now") {
            then("Get formatted date and time") {
                nowZoned.getRelativeFormattedTime(nowRelativeMessage) shouldBe nowRelativeMessage
            }
        }
        When("user uses 24-hour format") {
            every { DateFormat.is24HourFormat(context) } returns true
            then("Get formatted date and time") {
                zonedDateTime.getFormattedDateAndTime(context) shouldBe "May 30, 2024, 14:00"
            }
            then("get formatted time in 24-hour format") {
                zonedDateTime.getFormattedTime(context) shouldBe "14:00"
            }
        }
        When("user uses 12-hour format") {
            every { DateFormat.is24HourFormat(context) } returns false
            then("Get formatted date and time") {
                zonedDateTime.getFormattedDateAndTime(context) shouldBe "May 30, 2024, 2:00 PM"
            }
            When("showMinutes12HourFormat = true") {
                then("it should return the formatted time in 12-hour format with minutes") {
                    zonedDateTime.getFormattedTime(context, true) shouldBe "2:00 PM"
                }
            }
            When("showMinutes12HourFormat = false") {
                then("it should return the formatted time in 12-hour format without minutes") {
                    zonedDateTime.getFormattedTime(context, false) shouldBe "2 PM"
                }
            }
        }
        When("includeYear = true") {
            then("Get formatted date with year") {
                zonedDateTime.getFormattedDate(true) shouldBe "May 30, 2024"
            }
        }
        When("includeYear = false") {
            then("Get formatted date without year") {
                zonedDateTime.getFormattedDate(false) shouldBe "May 30"
            }
        }
    }

    given("A timestamp") {
        then("Get the Local Date") {
            timestampToLocalDate(timestamp) shouldBe customLocalDate
        }
    }

    given("A LocalDate") {
        When("It's Today") {
            then("Get relative day = Today") {
                todayLocalDate.getRelativeDayOrFull(context) shouldBe "Today"
            }
        }
        When("It's Yesterday") {
            then("Get relative day = Yesterday") {
                yesterdayLocalDate.getRelativeDayOrFull(context) shouldBe "Yesterday"
            }
        }
        When("It's Tomorrow") {
            then("Get relative day = Tomorrow") {
                tomorrowLocalDate.getRelativeDayOrFull(context) shouldBe "Tomorrow"
            }
        }
        When("It's a custom date") {
            then("Get relative day") {
                customLocalDate.getRelativeDayOrFull(context) shouldBe "Thu 30, May 24"
            }
            then("Get Name of day, MMM d") {
                customLocalDate.getRelativeDayAndShort(context) shouldBe "Thu, May 30"
            }
            then("Get Name of day, d/M") {
                customLocalDate.getRelativeDayAndMonthDay(context) shouldBe "Thursday 30/5"
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
