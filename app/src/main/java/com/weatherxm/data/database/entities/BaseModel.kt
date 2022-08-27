package com.weatherxm.data.database.entities

import androidx.room.ColumnInfo
import java.util.*

open class BaseModel {
    @ColumnInfo(name = "created_at")
    var createdAt: Date = Date(System.currentTimeMillis())

    @ColumnInfo(name = "updated_at")
    var updatedAt: Date = Date(System.currentTimeMillis())
}

