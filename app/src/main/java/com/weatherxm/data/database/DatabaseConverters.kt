package com.weatherxm.data.database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.weatherxm.data.WeatherData
import java.time.ZonedDateTime
import java.util.*

@ProvidedTypeConverter
class DatabaseConverters {

    @TypeConverter
    fun toZonedDateTime(timeInISO: String?): ZonedDateTime {
        return ZonedDateTime.parse(timeInISO)
    }

    @TypeConverter
    fun fromZonedDateTime(zonedDateTime: ZonedDateTime): String {
        return zonedDateTime.toString()
    }

    @TypeConverter
    fun toDate(dateLong: Long?): Date? {
        return dateLong?.let { Date(it) }
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }
}
