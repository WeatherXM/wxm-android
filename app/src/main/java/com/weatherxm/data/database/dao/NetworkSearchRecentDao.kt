package com.weatherxm.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.weatherxm.data.database.entities.NetworkSearchRecent
import java.util.Date

@Dao
interface NetworkSearchRecentDao : BaseDao<NetworkSearchRecent> {

    @Query("SELECT * FROM NetworkSearchRecent ORDER BY updated_at DESC")
    fun getAll(): List<NetworkSearchRecent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override fun insert(data: NetworkSearchRecent)

    @Query("DELETE FROM NetworkSearchRecent")
    fun deleteAll()

    @Query(
        "DELETE FROM NetworkSearchRecent " +
            "WHERE updated_at = " +
            "(SELECT updated_at FROM NetworkSearchRecent WHERE updated_at <= :updatedAt)"
    )
    fun deleteOutOfLimitRecents(updatedAt: Date)
}
