package com.weatherxm.ui.deeplinkrouter

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.weatherxm.R
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.DataError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.RemoteMessageType
import com.weatherxm.data.models.WXMRemoteMessage
import com.weatherxm.data.repository.ExplorerRepositoryImpl.Companion.EXCLUDE_PLACES
import com.weatherxm.ui.common.Contracts.ARG_REMOTE_MESSAGE
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.explorer.SearchResult
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.usecases.DeviceListUseCase
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class DeepLinkRouterViewModel(
    private val usecase: ExplorerUseCase,
    private val devicesUseCase: DeviceListUseCase,
    private val resources: Resources,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    companion object {
        const val STATIONS_PATH_SEGMENT = "stations"
        const val CELLS_PATH_SEGMENT = "cells"
    }

    private val onError = MutableLiveData<String>()

    // The Boolean here is to represent `showExplorerOnBack` argument.
    private val onDevice = MutableLiveData<Pair<UIDevice, Boolean>>()
    private val onCell = MutableLiveData<UICell>()
    private val onAnnouncement = MutableLiveData<String>()

    fun onError(): LiveData<String> = onError
    fun onDevice(): LiveData<Pair<UIDevice, Boolean>> = onDevice
    fun onCell(): LiveData<UICell> = onCell
    fun onAnnouncement(): LiveData<String> = onAnnouncement

    fun parseIntent(intent: Intent) {
        val remoteMessage = intent.parcelable<WXMRemoteMessage>(ARG_REMOTE_MESSAGE)
        if (remoteMessage != null) {
            handleRemoteMessage(remoteMessage)
        } else {
            handleUrl(intent.data)
        }
    }

    private fun handleRemoteMessage(remoteMessage: WXMRemoteMessage) {
        if (remoteMessage.type == RemoteMessageType.STATION) {
            handleDeviceNotification(remoteMessage)
        } else if (remoteMessage.type == RemoteMessageType.ANNOUNCEMENT) {
            handleAnnouncement(remoteMessage)
        }
    }

    private fun handleAnnouncement(remoteMessage: WXMRemoteMessage) {
        remoteMessage.url?.let {
            onAnnouncement.postValue(it)
        } ?: onError.postValue(resources.getString(R.string.could_not_parse_url))
    }

    private fun handleDeviceNotification(remoteMessage: WXMRemoteMessage) {
        viewModelScope.launch(dispatcher) {
            devicesUseCase.getUserDevices().getOrElse { mutableListOf() }.firstOrNull {
                it.id == remoteMessage.deviceId
            }?.let {
                onDevice.postValue(Pair(it, false))
            } ?: onError.postValue(resources.getString(R.string.error_device_not_found))
        }
    }

    private fun handleUrl(data: Uri?) {
        val pathSegments = data?.pathSegments ?: mutableListOf()

        // e.g. https://exporer.weatherxm.com/stations/my-weather-station
        if (pathSegments.size == 2 && pathSegments[0].equals(STATIONS_PATH_SEGMENT)) {
            searchForDevice(pathSegments[1])
        } else if (pathSegments.size == 2 && pathSegments[0].equals(CELLS_PATH_SEGMENT)) {
            searchForCell(pathSegments[1])
        } else {
            Timber.w("Error parsing the URL: ${data?.path}")
            onError.postValue(resources.getString(R.string.could_not_parse_url))
        }
    }

    private fun searchForCell(cellIndex: String) {
        viewModelScope.launch(dispatcher) {
            usecase.getCellInfo(cellIndex).onRight {
                onCell.postValue(it)
            }.onLeft {
                Timber.w("Error fetching cell info: $it")
                handleError(it)
            }
        }
    }

    private fun searchForDevice(normalizedDeviceName: String) {
        val deviceName = normalizedDeviceName.replace("-", " ")
        viewModelScope.launch(dispatcher) {
            usecase.networkSearch(deviceName, exact = true, exclude = EXCLUDE_PLACES).onRight {
                if (it.size == 1) {
                    getDeviceFromSearchResult(it[0])
                } else if (it.size > 1) {
                    onError.postValue(resources.getString(R.string.more_than_one_results))
                } else {
                    onError.postValue(resources.getString(R.string.share_url_no_results))
                }
            }.onLeft {
                handleError(it)
            }
        }
    }

    private suspend fun getDeviceFromSearchResult(searchResult: SearchResult) {
        devicesUseCase.getUserDevices().getOrElse { mutableListOf() }.firstOrNull {
            it.id == searchResult.stationId
        }?.let {
            onDevice.postValue(Pair(it, true))
        } ?: kotlin.run {
            usecase.getCellDevice(
                searchResult.stationCellIndex ?: String.empty(),
                searchResult.stationId ?: String.empty()
            ).onRight {
                it.cellCenter = searchResult.center
                onDevice.postValue(Pair(it, true))
            }.onLeft {
                handleError(it)
            }
        }
    }

    private fun handleError(failure: Failure) {
        onError.postValue(
            when (failure) {
                is ApiError.DeviceNotFound -> resources.getString(R.string.error_device_not_found)
                is DataError.CellNotFound -> resources.getString(R.string.error_cell_not_found)
                else -> failure.getDefaultMessage(R.string.error_reach_out)
            }
        )
    }
}
