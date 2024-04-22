package com.weatherxm.util

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import com.weatherxm.R
import com.weatherxm.data.DATE_FORMAT_FULL
import com.weatherxm.data.DATE_FORMAT_MONTH_DAY
import com.weatherxm.data.DATE_FORMAT_MONTH_SHORT
import com.weatherxm.data.HOUR_FORMAT_12H_FULL
import com.weatherxm.data.HOUR_FORMAT_12H_HOUR_ONLY
import com.weatherxm.data.HOUR_FORMAT_24H
import com.weatherxm.ui.common.empty
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateTimeHelper : KoinComponent {

    private val formatter24h: DateTimeFormatter by inject(named(HOUR_FORMAT_24H))
    private val formatter12hFull: DateTimeFormatter by inject(named(HOUR_FORMAT_12H_FULL))
    private val formatter12hHourOnly: DateTimeFormatter by inject(named(HOUR_FORMAT_12H_HOUR_ONLY))
    private val formatterMonthDay: DateTimeFormatter by inject(named(DATE_FORMAT_MONTH_DAY))
    private val formatterShort: DateTimeFormatter by inject(named(DATE_FORMAT_MONTH_SHORT))
    private val formatterFull: DateTimeFormatter by inject(named(DATE_FORMAT_FULL))

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

    fun ZonedDateTime?.getFormattedDate(includeYear: Boolean = false): String {
        return this?.let {
            if (includeYear) {
                "${month.getDisplayName(TextStyle.SHORT, Locale.US)} $dayOfMonth, $year"
            } else {
                "${month.getDisplayName(TextStyle.SHORT, Locale.US)} $dayOfMonth"
            }
        } ?: String.empty()

    }

    fun ZoneOffset.getFormattedOffset(): String {
        // In order to show +2:00 instead of +02:00 for example
        return this.toString().replaceFirst("0", String.empty())
    }

    fun ZoneOffset.isUTC(): Boolean {
        return this == UTC
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

    fun timestampToLocalDate(timestamp: Long): LocalDate {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    fun LocalDate.getRelativeDayOrFull(context: Context): String {
        return when {
            isToday() -> context.getString(R.string.today)
            isTomorrow() -> context.getString(R.string.tomorrow)
            isYesterday() -> context.getString(R.string.yesterday)
            else -> format(formatterFull)
        }
    }

    fun LocalDate.getRelativeDayAndMonthDay(context: Context): String {
        val relativeDay = when {
            isToday() -> context.getString(R.string.today)
            isTomorrow() -> context.getString(R.string.tomorrow)
            isYesterday() -> context.getString(R.string.yesterday)
            else -> null
        }
        val nameOfDay = dayOfWeek.getName(context)
        return if (relativeDay != null) {
            "$relativeDay, $nameOfDay ${format(formatterMonthDay)}"
        } else {
            "$nameOfDay ${format(formatterMonthDay)}"
        }
    }

    fun LocalDate.getRelativeDayAndShort(context: Context): String {
        val relativeDay = when {
            isToday() -> context.getString(R.string.today)
            isTomorrow() -> context.getString(R.string.tomorrow)
            isYesterday() -> context.getString(R.string.yesterday)
            else -> null
        }
        val nameOfDay = dayOfWeek.getShortName(context)
        return if (relativeDay != null) {
            "$relativeDay, $nameOfDay, ${format(formatterShort)}"
        } else {
            "$nameOfDay, ${format(formatterShort)}"
        }
    }
}
