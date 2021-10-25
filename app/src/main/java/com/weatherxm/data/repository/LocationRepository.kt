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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class LocationRepository : KoinComponent {
    companion object {
        const val TIMEOUT = 5000L
    }

    private val dataSource: LocationDataSource by inject()
    private var flow: Flow<Location>? = null

    @OptIn(DelicateCoroutinesApi::class)
    @RequiresPermission(ACCESS_FINE_LOCATION)
    private fun getLocationFlow(): Flow<Location> {
        Timber.d("Creating location flow in repository")
        if (flow == null) {
            flow = dataSource.getLocationUpdates()
                .shareIn(GlobalScope, SharingStarted.WhileSubscribed(TIMEOUT))
        }
        return flow!!
    }

    @RequiresPermission(ACCESS_FINE_LOCATION)
    suspend fun getLastLocation(): Location = dataSource.getLastLocation()

    @RequiresPermission(ACCESS_FINE_LOCATION)
    fun getLocationUpdates(): Flow<Location> = getLocationFlow().conflate()
}
