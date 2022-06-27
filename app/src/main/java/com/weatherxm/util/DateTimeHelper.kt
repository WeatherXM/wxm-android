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

    @Suppress("MagicNumber")
    fun getLast7Days(resHelper: ResourcesHelper): List<String> {
        val last7days = mutableListOf<String>()

        val zonedDateTime = ZonedDateTime.now()
        for (i in 7 downTo 0) {
            last7days.add(
                getRelativeDayFromISO(
                    resHelper,
                    zonedDateTime.minusDays(i.toLong()).toString(),
                    false
                )
            )
        }
        return last7days
    }

    fun getRelativeTimeFromISO(
        date: ZonedDateTime,
        defaultIfTooSoon: String? = null
    ): String {
        val now = ZonedDateTime.now(date.zone)

        // Too soon?
        if (Duration.between(now, date).toMinutes() < 1 && defaultIfTooSoon != null) {
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
}
