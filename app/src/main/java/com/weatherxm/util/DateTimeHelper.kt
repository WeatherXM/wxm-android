package com.weatherxm.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit

fun LocalDateTime.startOfHour(): LocalDateTime {
    return this.withMinute(0).withSecond(0).withNano(0)
}

fun LocalDateTime.endOfHour(): LocalDateTime {
    return this.plusHours(1).startOfHour()
}

fun LocalDateTime.endOfDay(): LocalDateTime {
    return this.apply {
        plusDays(1)
        withHour(0)
        withMinute(0)
        withSecond(0)
        withNano(0)
    }
}

fun LocalDateTime.millis(): Long {
    return TimeUnit.SECONDS.toMillis(this.atZone(ZoneOffset.systemDefault()).toEpochSecond())
}

fun fromMillis(millis: Long): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
}

fun LocalDateTime.formatDefault(): String {
    return format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
}
