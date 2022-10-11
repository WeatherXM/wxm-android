package com.weatherxm.util

import java.time.ZonedDateTime

fun ZonedDateTime.isYesterday(): Boolean {
    val now = ZonedDateTime.now(this.zone)
    return now.minusDays(1).dayOfYear == this.dayOfYear
}

fun ZonedDateTime.isToday(): Boolean {
    val now = ZonedDateTime.now(this.zone)
    return now.dayOfYear == this.dayOfYear
}

fun ZonedDateTime.isTomorrow(): Boolean {
    val now = ZonedDateTime.now(this.zone)
    return now.dayOfYear == this.minusDays(1).dayOfYear
}

fun ZonedDateTime.toISODate(): String = this.toLocalDate().toString()
