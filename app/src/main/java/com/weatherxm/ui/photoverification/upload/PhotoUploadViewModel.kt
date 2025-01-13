package com.weatherxm.ui.photoverification.upload

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.models.PhotoPresignedMetadata
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.DevicePhotoUseCase
import com.weatherxm.util.ImageFileHelper.getUriForFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PhotoUploadViewModel(
    val device: UIDevice,
    val photos: MutableList<StationPhoto>,
    private val usecase: DevicePhotoUseCase,
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
                }.onLeft {
                    // TODO: Handle Failure
                }
        }
    }
}
