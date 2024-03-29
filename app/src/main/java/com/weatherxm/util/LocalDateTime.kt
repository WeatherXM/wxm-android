package com.weatherxm.util

import java.time.Duration
import java.time.LocalDateTime

class LocalDateTimeRange(
    override val start: LocalDateTime,
    override val endInclusive: LocalDateTime,
    private val step: Duration? = Duration.ofHours(1)
) : ClosedRange<LocalDateTime>, Iterable<LocalDateTime> {

    fun start() = start

    fun end() = endInclusive

    fun all() = map { it }

    override fun iterator() = LocalDateTimeIterator(start, endInclusive, step)

    override fun equals(other: Any?): Boolean {
        return other is LocalDateTimeRange && start() == other.start() && end() == other.end()
    }

    override fun toString() = "LocalDateTimeRange[${start()}...${end()}]"

    override fun hashCode(): Int = toString().hashCode()

    infix fun step(duration: Duration) = LocalDateTimeRange(start(), end(), duration)
}

class LocalDateTimeIterator(
    start: LocalDateTime,
    private val endInclusive: LocalDateTime,
    private val step: Duration? = Duration.ofHours(1)
) : Iterator<LocalDateTime> {

    private var current = start

    override fun hasNext(): Boolean {
        return current.isBefore(endInclusive) || current.isEqual(endInclusive)
    }

    override fun next(): LocalDateTime {
        if (!hasNext()) {
            throw NoSuchElementException("Reached the end of this LocalDateTimeRange")
        }
        val next = current
        current = current.plus(step)
        return next
    }
}

operator fun LocalDateTime.rangeTo(other: LocalDateTime) = LocalDateTimeRange(this, other)
