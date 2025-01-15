package com.weatherxm.ui.photoverification.upload

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.PhotoPresignedMetadata
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.DevicePhotoUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.ImageFileHelper.getUriForFile
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PhotoUploadViewModel(
    val device: UIDevice,
    val photos: MutableList<StationPhoto>,
    private val usecase: DevicePhotoUseCase,
    private val resources: Resources,
    private val analytics: AnalyticsWrapper,
    val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val onPhotosPresignedMetadata =
        MutableLiveData<Resource<List<PhotoPresignedMetadata>>>()

    fun onPhotosPresignedMetadata(): LiveData<Resource<List<PhotoPresignedMetadata>>> =
        onPhotosPresignedMetadata

    fun getUrisOfLocalPhotos(context: Context): ArrayList<Uri> {
        val uris = arrayListOf<Uri>()
        photos.forEach {
            if (!it.localPath.isNullOrEmpty()) {
                uris.add(File(it.localPath).getUriForFile(context))
            }
        }
        return uris
    }

    fun prepareUpload() {
        viewModelScope.launch(dispatcher) {
            onPhotosPresignedMetadata.postValue(Resource.loading())
            usecase.getPhotosMetadataForUpload(device.id, photos.mapNotNull { it.localPath })
                .onRight {
                    onPhotosPresignedMetadata.postValue(Resource.success(it))
                }
                .onLeft {
                    analytics.trackEventFailure(it.code)
                    val errorMessage = when (it) {
                        is ApiError.DeviceNotFound -> {
                            resources.getString(R.string.error_device_not_found)
                        }
                        is ApiError.GenericError.JWTError.UnauthorizedError -> it.message
                        else -> it.getDefaultMessage(R.string.error_reach_out_short)
                    } ?: resources.getString(R.string.error_reach_out_short)
                    onPhotosPresignedMetadata.postValue(Resource.error(errorMessage))
                }
        }
    }
}
