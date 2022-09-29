package com.weatherxm.data.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateJsonAdapter : JsonAdapter<LocalDate>() {

    override fun fromJson(reader: JsonReader): LocalDate? {
        if (reader.peek() == null) {
            return reader.nextNull()
        }
        val string = reader.nextString()
        return LocalDate.parse(string)
    }

    override fun toJson(writer: JsonWriter, value: LocalDate?) {
        if (value == null) {
            writer.nullValue()
        } else {
            val string = value.format(DateTimeFormatter.ISO_LOCAL_DATE)
            writer.value(string)
        }
    }
}
