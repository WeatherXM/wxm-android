package com.weatherxm.ui.urlrouteractivity

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.weatherxm.R
import com.weatherxm.data.ApiError
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.ExplorerRepositoryImpl.Companion.EXCLUDE_PLACES
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.SearchResult
import com.weatherxm.usecases.DeviceDetailsUseCase
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
    private val devicesUseCase: DeviceDetailsUseCase by inject()
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
                    if (it.size == 1) {
                        getDevice(it[0])
                    } else if (it.size > 1) {
                        onError.postValue(resHelper.getString(R.string.more_than_one_results))
                    } else {
                        onError.postValue(resHelper.getString(R.string.share_url_no_results))
                    }
                }
                .onLeft {
                    onError.postValue(it.getDefaultMessage(R.string.error_reach_out))
                }
        }
    }

    private suspend fun getDevice(searchResult: SearchResult) {
        devicesUseCase.getUserDevices().getOrElse { mutableListOf() }.firstOrNull {
            it.id == searchResult.stationId
        }?.let {
            onDevice.postValue(it)
        } ?: kotlin.run {
            usecase.getCellDevice(searchResult.stationCellIndex ?: "", searchResult.stationId ?: "")
                .onRight {
                    it.cellCenter = searchResult.center
                    onDevice.postValue(it)
                }
                .onLeft {
                    handleError(it)
                }
        }
    }

    private fun handleError(failure: Failure) {
        onError.postValue(
            if (failure is ApiError.DeviceNotFound) {
                resHelper.getString(R.string.error_device_not_found)
            } else {
                failure.getDefaultMessage(R.string.error_reach_out)
            }
        )
    }
}
