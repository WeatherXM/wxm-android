package com.weatherxm.data.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.math.BigInteger
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class BigIntegerJsonAdapter : JsonAdapter<BigInteger>() {

    override fun fromJson(reader: JsonReader): BigInteger? {
        if (reader.peek() == null) {
            return reader.nextNull()
        }
        val string = reader.nextString()
        return BigInteger(string)
    }

    override fun toJson(writer: JsonWriter, value: BigInteger?) {
        if (value == null) {
            writer.nullValue()
        } else {
            val string = value.toString()
            writer.value(string)
        }
    }
}
