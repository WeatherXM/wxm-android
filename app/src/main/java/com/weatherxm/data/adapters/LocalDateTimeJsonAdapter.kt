package com.weatherxm.data.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocalDateTimeJsonAdapter : JsonAdapter<LocalDateTime>() {

    override fun fromJson(reader: JsonReader): LocalDateTime? {
        if (reader.peek() == null) {
            return reader.nextNull()
        }
        val string = reader.nextString()
        return LocalDateTime.parse(string)
    }

    override fun toJson(writer: JsonWriter, value: LocalDateTime?) {
        if (value == null) {
            writer.nullValue()
        } else {
            val string = value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            writer.value(string)
        }
    }
}
