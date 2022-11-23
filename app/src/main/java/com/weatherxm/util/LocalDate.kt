package com.weatherxm.util

import java.time.LocalDate

class LocalDateRange(
    override val start: LocalDate,
    override val endInclusive: LocalDate
) : ClosedRange<LocalDate>, Iterable<LocalDate> {

    fun start() = start

    fun end() = endInclusive

    fun all() = map { it }

    override fun iterator() = LocalDateIterator(start, endInclusive)

    override fun equals(other: Any?): Boolean {
        return other is LocalDateRange && start() == other.start() && end() == other.end()
    }

    override fun toString() = "LocalDateRange[${start()}...${end()}]"

    override fun hashCode(): Int = toString().hashCode()
}

class LocalDateIterator(
    start: LocalDate,
    private val endInclusive: LocalDate
) : Iterator<LocalDate> {

    private var current = start

    override fun hasNext(): Boolean {
        return current.toEpochDay() <= endInclusive.toEpochDay()
    }

    override fun next(): LocalDate {
        if (!hasNext()) {
            throw NoSuchElementException("Reached the end of this LocalDateRange")
        }
        val next = current
        current = current.plusDays(1)
        return next
    }
}

operator fun LocalDate.rangeTo(other: LocalDate) = LocalDateRange(this, other)

fun LocalDate.isYesterday(): Boolean {
    val now = LocalDate.now()
    return now.minusDays(1).dayOfYear == this.dayOfYear
}

fun LocalDate.isToday(): Boolean {
    val now = LocalDate.now()
    return now.dayOfYear == this.dayOfYear
}

fun LocalDate.isTomorrow(): Boolean {
    val now = LocalDate.now()
    return now.dayOfYear == this.minusDays(1).dayOfYear
}