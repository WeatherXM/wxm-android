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
