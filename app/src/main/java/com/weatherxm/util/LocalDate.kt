package com.weatherxm.util

import java.time.LocalDate

class LocalDateRange(
    override val start: LocalDate,
    override val endInclusive: LocalDate
) : ClosedRange<LocalDate>, Iterable<LocalDate> {

    override fun iterator() = LocalDateIterator(start, endInclusive)

    override fun equals(other: Any?): Boolean {
        return other is LocalDateRange && start == other.start && endInclusive == other.endInclusive
    }

    override fun toString() = "LocalDateRange($start...$endInclusive]"

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
