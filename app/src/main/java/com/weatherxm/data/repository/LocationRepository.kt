package com.weatherxm.data.repository

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.annotation.RequiresPermission
import com.weatherxm.data.Location
import com.weatherxm.data.datasource.LocationDataSource
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.shareIn
import timber.log.Timber

interface LocationRepository {
    fun getLocationFlow(): Flow<Location>
    suspend fun getLastLocation(): Location
    fun getLocationUpdates(): Flow<Location>
}

class LocationRepositoryImpl(private val dataSource: LocationDataSource) : LocationRepository {
    companion object {
        const val TIMEOUT = 5000L
    }

    private var flow: Flow<Location>? = null

    @OptIn(DelicateCoroutinesApi::class)
    @RequiresPermission(ACCESS_FINE_LOCATION)
    override fun getLocationFlow(): Flow<Location> {
        Timber.d("Creating location flow in repository")
        if (flow == null) {
            flow = dataSource.getLocationUpdates()
                .shareIn(GlobalScope, SharingStarted.WhileSubscribed(TIMEOUT))
        }
        return flow!!
    }

    @RequiresPermission(ACCESS_FINE_LOCATION)
    override suspend fun getLastLocation(): Location = dataSource.getLastLocation()

    @RequiresPermission(ACCESS_FINE_LOCATION)
    override fun getLocationUpdates(): Flow<Location> = getLocationFlow().conflate()
}
