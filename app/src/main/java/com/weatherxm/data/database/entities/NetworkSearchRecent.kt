package com.weatherxm.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.weatherxm.data.Connectivity

@Entity(primaryKeys = ["name", "lat", "lon"])
data class NetworkSearchRecent(
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "lat")
    val lat: Double,
    @ColumnInfo(name = "lon")
    val lon: Double,
    @ColumnInfo(name = "address_place")
    val addressPlace: String?,
    @ColumnInfo(name = "station_connectivity")
    val stationConnectivity: Connectivity?,
    @ColumnInfo(name = "station_cell_index")
    val stationCellIndex: String?,
    @ColumnInfo(name = "station_id")
    val stationId: String?,
) : BaseModel()
