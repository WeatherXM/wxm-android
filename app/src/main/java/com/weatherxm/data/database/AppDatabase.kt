package com.weatherxm.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.weatherxm.data.database.dao.DeviceHistoryDao
import com.weatherxm.data.database.entities.DeviceHourlyHistory

@Database(entities = [DeviceHourlyHistory::class], version = 3)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceHistoryDao(): DeviceHistoryDao
}
