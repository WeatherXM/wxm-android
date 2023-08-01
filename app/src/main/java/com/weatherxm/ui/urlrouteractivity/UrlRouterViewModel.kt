package com.weatherxm.ui.urlrouteractivity

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError
import com.weatherxm.data.repository.ExplorerRepositoryImpl.Companion.EXCLUDE_PLACES
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.SearchResult
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UrlRouterViewModel : ViewModel(), KoinComponent {
    companion object {
        const val STATIONS_PATH_SEGMENT = "stations"
    }

    private val usecase: ExplorerUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    private val onError = MutableLiveData<String>()
    private val onDevice = MutableLiveData<UIDevice>()

    fun onError(): LiveData<String> = onError
    fun onDevice(): LiveData<UIDevice> = onDevice

    fun parseUrl(uri: Uri?) {
        val pathSegments = uri?.pathSegments ?: mutableListOf()

        // e.g. https://exporer.weatherxm.com/stations/my-weather-station
        if (pathSegments.size == 2 && pathSegments[0].equals(STATIONS_PATH_SEGMENT)) {
            searchForDevice(pathSegments[1])
        } else {
            onError.postValue(resHelper.getString(R.string.could_not_parse_url))
        }
    }

    private fun searchForDevice(normalizedDeviceName: String) {
        val deviceName = normalizedDeviceName.replace("-", " ")
        viewModelScope.launch {
            usecase.networkSearch(deviceName, exact = true, exclude = EXCLUDE_PLACES)
                .onRight {
                    // TODO: Remove this if we go with the "exact" parameter and use it.size == 1
                    if (it.isNotEmpty()) {
                        getDevice(it[0])
                    } else {
                        onError.postValue(resHelper.getString(R.string.more_than_one_results))
                    }
                }
                .onLeft {
                    onError.postValue(it.getDefaultMessage(R.string.error_reach_out))
                }
        }
    }

    private suspend fun getDevice(searchResult: SearchResult) {
        usecase.getCellDevice(searchResult.stationCellIndex ?: "", searchResult.stationId ?: "")
            .onRight {
                it.cellCenter = searchResult.center
                onDevice.postValue(it)
            }
            .onLeft {
                onError.postValue(
                    if (it is ApiError.DeviceNotFound) {
                        resHelper.getString(R.string.error_device_not_found)
                    } else {
                        it.getDefaultMessage(R.string.error_reach_out)
                    }
                )
            }
    }
}
