package com.weatherxm.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

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
    val bundleName: String?,
    val bundleTitle: String?,
    val connectivity: String?,
    @ColumnInfo(name = "ws_model")
    val wsModel: String?,
    @ColumnInfo(name = "gw_model")
    val gwModel: String?,
    @ColumnInfo(name = "hw_class")
    val hwClass: String?,
    @ColumnInfo(name = "station_cell_index")
    val stationCellIndex: String?,
    @ColumnInfo(name = "station_id")
    val stationId: String?,
) : BaseModel()
