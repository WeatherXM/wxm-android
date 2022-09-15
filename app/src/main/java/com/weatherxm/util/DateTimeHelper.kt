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
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@Suppress("TooManyFunctions")
object DateTimeHelper : KoinComponent {

    private val formatter24h: DateTimeFormatter by inject(named(HOUR_FORMAT_24H))
    private val formatter12hFull: DateTimeFormatter by inject(named(HOUR_FORMAT_12H_FULL))
    private val formatter12hHourOnly: DateTimeFormatter by inject(named(HOUR_FORMAT_12H_HOUR_ONLY))
    private val formatterMonthDay: DateTimeFormatter by inject(named(DATE_FORMAT_MONTH_DAY))

    fun getNowInTimezone(timezone: String? = null): ZonedDateTime {
        if (timezone == null) {
            return ZonedDateTime.now()
        }
        val tz = ZoneId.of(timezone)
        return ZonedDateTime.now(tz)
    }

    fun getTimezone(): String {
        return ZoneId.systemDefault().toString()
    }

    fun getLocalDate(timeInISO: String?): LocalDate {
        return ZonedDateTime.parse(timeInISO).toLocalDate()
    }

    fun getFormattedDate(zonedDateTime: ZonedDateTime): String {
        return getLocalDate(zonedDateTime.toString()).toString()
    }

    fun dateToLocalDate(date: String?): LocalDate {
        return LocalDate.parse(date)
    }

    fun getFormattedDate(timeInISO: String?): String {
        return ZonedDateTime.parse(timeInISO).toLocalDate().toString()
    }

    fun getRelativeDayFromISO(
        resHelper: ResourcesHelper,
        timeInISO: String,
        fullName: Boolean
    ): String {
        val zonedDateTime = ZonedDateTime.parse(timeInISO)
        return when {
            zonedDateTime.isToday() -> resHelper.getString(R.string.today)
            zonedDateTime.isTomorrow() -> resHelper.getString(R.string.tomorrow)
            zonedDateTime.isYesterday() -> resHelper.getString(R.string.yesterday)
            else -> {
                val nameOfDay = if (fullName) {
                    zonedDateTime.dayOfWeek.getName(resHelper)
                } else {
                    zonedDateTime.dayOfWeek.getShortName(resHelper)
                }

                "$nameOfDay ${zonedDateTime.format(formatterMonthDay)}"
            }
        }
    }

    fun getRelativeDayFromLocalDate(
        resHelper: ResourcesHelper,
        localDate: LocalDate,
        fullName: Boolean = false
    ): String {
        return when {
            localDate.isToday() -> resHelper.getString(R.string.today)
            localDate.isTomorrow() -> resHelper.getString(R.string.tomorrow)
            localDate.isYesterday() -> resHelper.getString(R.string.yesterday)
            else -> {
                val nameOfDay = if (fullName) {
                    localDate.dayOfWeek.getName(resHelper)
                } else {
                    localDate.dayOfWeek.getShortName(resHelper)
                }
                "$nameOfDay ${localDate.format(formatterMonthDay)}"
            }
        }
    }

    fun getRelativeTimeFromISO(
        date: ZonedDateTime,
        defaultIfTooSoon: String? = null
    ): String {
        val now = ZonedDateTime.now(date.zone)

        // Too soon?
        if (Duration.between(date, now).toMinutes() < 1 && defaultIfTooSoon != null) {
            return defaultIfTooSoon
        }

        return DateUtils.getRelativeTimeSpanString(
            date.toInstant().toEpochMilli(),
            now.toInstant().toEpochMilli(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

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

    fun getSimplifiedDate(fullDate: String): String {
        val localDate = LocalDate.parse(fullDate)
        return localDate.format(formatterMonthDay)
    }

    fun getShortNameOfDayFromLocalDate(resHelper: ResourcesHelper, fullDate: String): String {
        val localDate = LocalDate.parse(fullDate)
        return localDate.dayOfWeek.getShortName(resHelper)
    }

    private fun ZonedDateTime.isYesterday(): Boolean {
        val now = ZonedDateTime.now(this.zone)
        return now.minusDays(1).dayOfYear == this.dayOfYear
    }

    private fun ZonedDateTime.isToday(): Boolean {
        val now = ZonedDateTime.now(this.zone)
        return now.dayOfYear == this.dayOfYear
    }

    fun ZonedDateTime.isTomorrow(): Boolean {
        val now = ZonedDateTime.now(this.zone)
        return now.dayOfYear == this.minusDays(1).dayOfYear
    }

    private fun LocalDate.isYesterday(): Boolean {
        val now = LocalDate.now()
        return now.minusDays(1).dayOfYear == this.dayOfYear
    }

    private fun LocalDate.isToday(): Boolean {
        val now = LocalDate.now()
        return now.dayOfYear == this.dayOfYear
    }

    fun LocalDate.isTomorrow(): Boolean {
        val now = LocalDate.now()
        return now.dayOfYear == this.minusDays(1).dayOfYear
    }

    private fun DayOfWeek.getName(resHelper: ResourcesHelper): String {
        return when (this) {
            DayOfWeek.MONDAY -> resHelper.getString(R.string.monday)
            DayOfWeek.TUESDAY -> resHelper.getString(R.string.tuesday)
            DayOfWeek.WEDNESDAY -> resHelper.getString(R.string.wednesday)
            DayOfWeek.THURSDAY -> resHelper.getString(R.string.thursday)
            DayOfWeek.FRIDAY -> resHelper.getString(R.string.friday)
            DayOfWeek.SATURDAY -> resHelper.getString(R.string.saturday)
            DayOfWeek.SUNDAY -> resHelper.getString(R.string.sunday)
        }
    }

    private fun DayOfWeek.getShortName(resHelper: ResourcesHelper): String {
        return when (this) {
            DayOfWeek.MONDAY -> resHelper.getString(R.string.mon)
            DayOfWeek.TUESDAY -> resHelper.getString(R.string.tue)
            DayOfWeek.WEDNESDAY -> resHelper.getString(R.string.wed)
            DayOfWeek.THURSDAY -> resHelper.getString(R.string.thu)
            DayOfWeek.FRIDAY -> resHelper.getString(R.string.fri)
            DayOfWeek.SATURDAY -> resHelper.getString(R.string.sat)
            DayOfWeek.SUNDAY -> resHelper.getString(R.string.sun)
        }
    }

    fun getDateRangeFromToday(n: Int, includeToday: Boolean = true): LocalDateRange {
        val today = LocalDate.now()
        return if (n > 0) {
            val offset = if (includeToday) 1L else 0L
            LocalDateRange(
                today.minusDays(offset),
                today.plusDays(n.toLong() - offset)
            )
        } else if (n < 0) {
            val offset = if (includeToday) 0L else 1L
            LocalDateRange(
                today.minusDays(n.absoluteValue.toLong() + offset),
                today.minusDays(offset)
            )
        } else {
            throw IllegalArgumentException("n must be a non-zero negative or positive number")
        }
    }

    @Suppress("EqualsOrHashCode")
    class LocalDateRange(
        override val start: LocalDate,
        override val endInclusive: LocalDate
    ) : ClosedRange<LocalDate>, Iterable<LocalDate> {
        override fun iterator(): Iterator<LocalDate> {
            return LocalDateIterator(start, endInclusive)
        }

        override fun equals(other: Any?): Boolean {
            if (other is LocalDateRange) {
                this.forEachIndexed { index, date ->
                    if (!date.isEqual(other.elementAt(index))) {
                        return false
                    }
                }
                return true
            } else {
                return false
            }
        }

        override fun toString(): String = "DateRange($start...$endInclusive]"

        override fun hashCode(): Int = toString().hashCode()
    }

    class LocalDateIterator(
        start: LocalDate,
        private val endInclusive: LocalDate
    ) : Iterator<LocalDate> {

        private var current = start

        override fun hasNext(): Boolean {
            return current.isBefore(endInclusive)
        }

        override fun next(): LocalDate {
            if (!hasNext()) {
                throw NoSuchElementException("Reached the end of the LocalDateRange")
            }
            current = current.plusDays(1)
            return current
        }
    }
}
