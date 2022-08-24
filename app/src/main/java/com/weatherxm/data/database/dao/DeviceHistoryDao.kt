package com.weatherxm.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.weatherxm.data.database.entities.DeviceHourlyHistory

@Dao
interface DeviceHistoryDao : BaseDao<DeviceHourlyHistory> {
    @Query(
        "SELECT * FROM DeviceHourlyHistory " +
            "WHERE device_id = (:deviceId) " +
            "AND timestamp BETWEEN date((:fromDate)) AND date((:toDate)) " +
            "ORDER BY date(timestamp)"
    )
    fun getInRange(deviceId: String, fromDate: String, toDate: String): List<DeviceHourlyHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override fun insert(data: DeviceHourlyHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    override fun insertAll(data: List<DeviceHourlyHistory>)

    @Query(
        "DELETE FROM DeviceHourlyHistory " +
            "WHERE device_id = (:deviceId) AND date(timestamp) < date((:upToDate))"
    )
    fun deleteInRange(deviceId: String, upToDate: String)
}
