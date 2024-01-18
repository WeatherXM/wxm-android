package com.weatherxm.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.weatherxm.data.database.dao.DeviceHistoryDao
import com.weatherxm.data.database.dao.NetworkSearchRecentDao
import com.weatherxm.data.database.entities.DeviceHourlyHistory
import com.weatherxm.data.database.entities.NetworkSearchRecent

@Database(
    entities = [DeviceHourlyHistory::class, NetworkSearchRecent::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceHistoryDao(): DeviceHistoryDao
    abstract fun networkSearchRecentDao(): NetworkSearchRecentDao
}
