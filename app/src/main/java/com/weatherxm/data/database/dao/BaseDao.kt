package com.weatherxm.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.weatherxm.data.database.entities.BaseModel
import java.util.Date

@Dao
interface BaseDao<T> where T: BaseModel {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    fun insertAll(data: List<T>)

    companion object {

        open class DAOWrapper<P, T>(
            private val daoInstance: T
        ) where T : BaseDao<P>, P : BaseModel {

            fun insertWithTimestamp(data: P) {
                data.updatedAt = Date(System.currentTimeMillis())
                this@DAOWrapper.daoInstance.insert(data)
            }

            fun insertAllWithTimestamp(data: List<P>) {
                data.forEach {
                    it.updatedAt = Date(System.currentTimeMillis())
                }
                this@DAOWrapper.daoInstance.insertAll(data)
            }
        }

    }
}
