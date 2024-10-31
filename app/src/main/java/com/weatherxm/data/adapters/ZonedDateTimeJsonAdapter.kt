package com.weatherxm.data.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ZonedDateTimeJsonAdapter : JsonAdapter<ZonedDateTime>() {

    override fun fromJson(reader: JsonReader): ZonedDateTime? {
        val nextToken = reader.peek()
        if (nextToken == null || nextToken == JsonReader.Token.NULL) {
            return reader.nextNull()
        }
        val string = reader.nextString()
        return ZonedDateTime.parse(string)
    }

    override fun toJson(writer: JsonWriter, value: ZonedDateTime?) {
        if (value == null) {
            writer.nullValue()
        } else {
            val string = value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            writer.value(string)
        }
    }
}
