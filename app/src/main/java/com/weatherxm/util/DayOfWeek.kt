package com.weatherxm.util

import com.weatherxm.R
import java.time.DayOfWeek

fun DayOfWeek.getName(): Int {
    return when (this) {
        DayOfWeek.MONDAY -> R.string.monday
        DayOfWeek.TUESDAY -> R.string.tuesday
        DayOfWeek.WEDNESDAY -> R.string.wednesday
        DayOfWeek.THURSDAY -> R.string.thursday
        DayOfWeek.FRIDAY -> R.string.friday
        DayOfWeek.SATURDAY -> R.string.saturday
        DayOfWeek.SUNDAY -> R.string.sunday
    }
}

fun DayOfWeek.getShortName(): Int {
    return when (this) {
        DayOfWeek.MONDAY -> R.string.mon
        DayOfWeek.TUESDAY -> R.string.tue
        DayOfWeek.WEDNESDAY -> R.string.wed
        DayOfWeek.THURSDAY -> R.string.thu
        DayOfWeek.FRIDAY -> R.string.fri
        DayOfWeek.SATURDAY -> R.string.sat
        DayOfWeek.SUNDAY -> R.string.sun
    }
}

fun DayOfWeek.getFirstLetter(): Int {
    return when (this) {
        DayOfWeek.MONDAY -> R.string.mon_first_letter
        DayOfWeek.TUESDAY -> R.string.tue_first_letter
        DayOfWeek.WEDNESDAY -> R.string.wed_first_letter
        DayOfWeek.THURSDAY -> R.string.thu_first_letter
        DayOfWeek.FRIDAY -> R.string.fri_first_letter
        DayOfWeek.SATURDAY -> R.string.sat_first_letter
        DayOfWeek.SUNDAY -> R.string.sun_first_letter
    }
}
