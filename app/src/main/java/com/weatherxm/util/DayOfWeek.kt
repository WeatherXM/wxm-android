package com.weatherxm.util

import android.content.Context
import com.weatherxm.R
import java.time.DayOfWeek

fun DayOfWeek.getName(context: Context): String {
    return when (this) {
        DayOfWeek.MONDAY -> context.getString(R.string.monday)
        DayOfWeek.TUESDAY -> context.getString(R.string.tuesday)
        DayOfWeek.WEDNESDAY -> context.getString(R.string.wednesday)
        DayOfWeek.THURSDAY -> context.getString(R.string.thursday)
        DayOfWeek.FRIDAY -> context.getString(R.string.friday)
        DayOfWeek.SATURDAY -> context.getString(R.string.saturday)
        DayOfWeek.SUNDAY -> context.getString(R.string.sunday)
    }
}

fun DayOfWeek.getShortName(context: Context): String {
    return when (this) {
        DayOfWeek.MONDAY -> context.getString(R.string.mon)
        DayOfWeek.TUESDAY -> context.getString(R.string.tue)
        DayOfWeek.WEDNESDAY -> context.getString(R.string.wed)
        DayOfWeek.THURSDAY -> context.getString(R.string.thu)
        DayOfWeek.FRIDAY -> context.getString(R.string.fri)
        DayOfWeek.SATURDAY -> context.getString(R.string.sat)
        DayOfWeek.SUNDAY -> context.getString(R.string.sun)
    }
}

fun DayOfWeek.getFirstLetter(context: Context): String {
    return when (this) {
        DayOfWeek.MONDAY -> context.getString(R.string.mon_first_letter)
        DayOfWeek.TUESDAY -> context.getString(R.string.tue_first_letter)
        DayOfWeek.WEDNESDAY -> context.getString(R.string.wed_first_letter)
        DayOfWeek.THURSDAY -> context.getString(R.string.thu_first_letter)
        DayOfWeek.FRIDAY -> context.getString(R.string.fri_first_letter)
        DayOfWeek.SATURDAY -> context.getString(R.string.sat_first_letter)
        DayOfWeek.SUNDAY -> context.getString(R.string.sun_first_letter)
    }
}
