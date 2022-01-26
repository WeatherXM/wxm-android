package com.weatherxm.util

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import com.weatherxm.R
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun getNowInTimezone(timezone: String?): ZonedDateTime {
    if (timezone == null) {
        return ZonedDateTime.now()
    }
    val tz = ZoneId.of(timezone)
    return ZonedDateTime.now(tz)
}

fun getFormattedDate(timeInISO: String?): String {
    return ZonedDateTime.parse(timeInISO).toLocalDate().toString()
}

fun getRelativeDayFromISO(
    resHelper: ResourcesHelper,
    timeInISO: String,
    includeDate: Boolean,
    fullName: Boolean
): String {
    val zonedDateTime = ZonedDateTime.parse(timeInISO)

    val textToReturn = when {
        isToday(timeInISO) -> {
            resHelper.getString(R.string.today)
        }
        isTomorrow(timeInISO) -> {
            resHelper.getString(R.string.tomorrow)
        }
        isYesterday(timeInISO) -> {
            resHelper.getString(R.string.yesterday)
        }
        else -> {
            val nameOfDay = if (fullName) {
                getNameOfDayOfWeek(resHelper, zonedDateTime.dayOfWeek.value)
            } else {
                getShortNameOfDayOfWeek(resHelper, zonedDateTime.dayOfWeek.value)
            }

            if (!includeDate) {
                nameOfDay
            } else {
                "$nameOfDay ${zonedDateTime.dayOfMonth}/${zonedDateTime.monthValue}"
            }
        }
    }

    return textToReturn
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
                includeDate = true,
                fullName = false
            )
        )
    }
    return last7days
}

fun getRelativeTimeFromISO(timeInISO: String): String {
    val zonedDateTime = ZonedDateTime.parse(timeInISO)
    val now = ZonedDateTime.now(zonedDateTime.zone)
    val oldTimeInMillis = zonedDateTime.toInstant().toEpochMilli()
    val nowInMillis = now.toInstant().toEpochMilli()

    return DateUtils.getRelativeTimeSpanString(
        oldTimeInMillis,
        nowInMillis,
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}

// TODO: Maybe inject this. Also check if multiple instances are being created each time
fun getHourMinutesFromISO(context: Context, timeInISO: String): String {
    val zonedDateTime = ZonedDateTime.parse(timeInISO)
    return if (DateFormat.is24HourFormat(context)) {
        zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } else {
        zonedDateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
    }
}

fun isYesterday(timeInISO: String): Boolean {
    val zonedDateTime = ZonedDateTime.parse(timeInISO)
    val now = ZonedDateTime.now(zonedDateTime.zone)

    return now.minusDays(1).dayOfYear == zonedDateTime.dayOfYear
}

fun isToday(timeInISO: String): Boolean {
    val zonedDateTime = ZonedDateTime.parse(timeInISO)
    val now = ZonedDateTime.now(zonedDateTime.zone)

    return now.dayOfYear == zonedDateTime.dayOfYear
}

fun isTomorrow(timeInISO: String): Boolean {
    val zonedDateTime = ZonedDateTime.parse(timeInISO)
    val now = ZonedDateTime.now(zonedDateTime.zone)

    return now.dayOfYear == zonedDateTime.minusDays(1).dayOfYear
}

@Suppress("MagicNumber")
fun getNameOfDayOfWeek(resHelper: ResourcesHelper, dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> resHelper.getString(R.string.monday)
        2 -> resHelper.getString(R.string.tuesday)
        3 -> resHelper.getString(R.string.wednesday)
        4 -> resHelper.getString(R.string.thursday)
        5 -> resHelper.getString(R.string.friday)
        6 -> resHelper.getString(R.string.saturday)
        7 -> resHelper.getString(R.string.sunday)
        else -> ""
    }
}

@Suppress("MagicNumber")
fun getShortNameOfDayOfWeek(resHelper: ResourcesHelper, dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> resHelper.getString(R.string.mon)
        2 -> resHelper.getString(R.string.tue)
        3 -> resHelper.getString(R.string.wed)
        4 -> resHelper.getString(R.string.thu)
        5 -> resHelper.getString(R.string.fri)
        6 -> resHelper.getString(R.string.sat)
        7 -> resHelper.getString(R.string.sun)
        else -> ""
    }
}
