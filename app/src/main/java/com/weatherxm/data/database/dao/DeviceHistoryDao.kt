package com.weatherxm.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.weatherxm.data.database.entities.DeviceHourlyHistory

@Dao
interface DeviceHistoryDao : BaseDao<DeviceHourlyHistory> {
    /*
    * The `BETWEEN` on Dates is inclusive but the `toDate` acts like it starts on midnight therefore
    * no data are being returned on toDate as timestamp will be always after midnight.
    *
    * This will be fixed in a future version where we will save timestamps and check timestamps and
    * not dates.
    *
    * https://stackoverflow.com/questions/16347649/sql-between-not-inclusive
    * https://sqlblog.org/2009/10/16/bad-habits-to-kick-mis-handling-date-range-queries
    *
     */
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
