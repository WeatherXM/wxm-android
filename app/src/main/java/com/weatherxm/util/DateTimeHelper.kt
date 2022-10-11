package com.weatherxm.util

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import com.weatherxm.R
import com.weatherxm.data.DATE_FORMAT_MONTH_DAY
import com.weatherxm.data.HOUR_FORMAT_12H_FULL
import com.weatherxm.data.HOUR_FORMAT_12H_HOUR_ONLY
import com.weatherxm.data.HOUR_FORMAT_24H
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

object DateTimeHelper : KoinComponent {

    private val formatter24h: DateTimeFormatter by inject(named(HOUR_FORMAT_24H))
    private val formatter12hFull: DateTimeFormatter by inject(named(HOUR_FORMAT_12H_FULL))
    private val formatter12hHourOnly: DateTimeFormatter by inject(named(HOUR_FORMAT_12H_HOUR_ONLY))
    private val formatterMonthDay: DateTimeFormatter by inject(named(DATE_FORMAT_MONTH_DAY))

    fun getHourMinutesFromISO(
        context: Context,
        timeInISO: String,
        showMinutes12HourFormat: Boolean = true
    ): String {
        val zonedDateTime = ZonedDateTime.parse(timeInISO)
        return if (DateFormat.is24HourFormat(context)) {
            zonedDateTime.format(formatter24h)
        } else {
            if (showMinutes12HourFormat) {
                zonedDateTime.format(formatter12hFull)
            } else {
                zonedDateTime.format(formatter12hHourOnly)
            }
        }
    }

    fun LocalDateTime.getFormattedTime(
        context: Context,
        showMinutes12HourFormat: Boolean = true
    ): String {
        return if (DateFormat.is24HourFormat(context)) {
            format(formatter24h)
        } else {
            if (showMinutes12HourFormat) {
                format(formatter12hFull)
            } else {
                format(formatter12hHourOnly)
            }
        }
    }

    fun ZonedDateTime.getFormattedDay(context: Context, showFullName: Boolean = false): String {
        return when {
            isToday() -> context.getString(R.string.today)
            isTomorrow() -> context.getString(R.string.tomorrow)
            isYesterday() -> context.getString(R.string.yesterday)
            else -> {
                val nameOfDay = if (showFullName) {
                    dayOfWeek.getName(context)
                } else {
                    dayOfWeek.getShortName(context)
                }
                "$nameOfDay ${format(formatterMonthDay)}"
            }
        }
    }

    fun ZonedDateTime.getFormattedTime(
        context: Context,
        showMinutesIn12HourFormat: Boolean = true
    ): String {
        return if (DateFormat.is24HourFormat(context)) {
            format(formatter24h)
        } else {
            if (showMinutesIn12HourFormat) {
                format(formatter12hFull)
            } else {
                format(formatter12hHourOnly)
            }
        }
    }

    fun ZonedDateTime.getRelativeFormattedTime(fallbackIfTooSoon: String? = null): String {
        val now = ZonedDateTime.now(zone)

        // Too soon?
        if (Duration.between(this, now).toMinutes() < 1 && fallbackIfTooSoon != null) {
            return fallbackIfTooSoon
        }

        return DateUtils.getRelativeTimeSpanString(
            toInstant().toEpochMilli(),
            now.toInstant().toEpochMilli(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    fun LocalDate.getFormattedRelativeDay(
        context: Context,
        fullName: Boolean = false
    ): String {
        return when {
            isToday() -> context.getString(R.string.today)
            isTomorrow() -> context.getString(R.string.tomorrow)
            isYesterday() -> context.getString(R.string.yesterday)
            else -> {
                val nameOfDay = if (fullName) {
                    dayOfWeek.getName(context)
                } else {
                    dayOfWeek.getShortName(context)
                }
                "$nameOfDay ${format(formatterMonthDay)}"
            }
        }
    }

    fun getDateRangeFromToday(n: Int, includeToday: Boolean = true): LocalDateRange {
        if (n == 0) {
            throw IllegalArgumentException("n must be a non-zero negative or positive number")
        }
        val today = LocalDate.now()
        val offset = if (includeToday) 0L else 1L
        val start = if (n > 0) today.plusDays(offset) else today.minusDays(n.absoluteValue + offset)
        val end = if (n > 0) today.plusDays(n + offset) else today.minusDays(offset)
        return start..end
    }
}
