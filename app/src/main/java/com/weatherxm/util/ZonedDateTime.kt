package com.weatherxm.util

import java.time.ZonedDateTime

fun ZonedDateTime.isToday(): Boolean {
    val now = ZonedDateTime.now(this.zone)
    return now.dayOfYear == this.dayOfYear
}

fun ZonedDateTime.toISODate(): String = this.toLocalDate().toString()

fun ZonedDateTime.isSameDayAndHour(targetTimestamp: ZonedDateTime): Boolean {
    return dayOfYear == targetTimestamp.dayOfYear && hour == targetTimestamp.hour
}
