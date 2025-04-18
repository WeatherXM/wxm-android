package com.weatherxm.ui.photoverification.gallery

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.ui.common.PhotoSource
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.DevicePhotoUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class PhotoGalleryViewModel(
    val device: UIDevice,
    val photos: MutableList<StationPhoto>,
    val fromClaiming: Boolean,
    private val usecase: DevicePhotoUseCase,
    val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val onPhotosNumber = MutableLiveData(photos.size)
    private val onDeletingPhotoStatus = MutableLiveData<Resource<Unit>>()
    private val _onPhotos = mutableStateListOf<StationPhoto>().apply {
        addAll(photos)
    }
    val onPhotos: List<StationPhoto> = _onPhotos

    fun onPhotosNumber(): LiveData<Int> = onPhotosNumber
    fun onDeletingPhotoStatus(): LiveData<Resource<Unit>> = onDeletingPhotoStatus

    fun getLocalPhotosNumber() = photos.filter { it.localPath != null }.size

    fun addPhoto(path: String, photoSource: PhotoSource) {
        if (path.isNotEmpty() && photos.firstOrNull { it.localPath == path } == null) {
            val stationPhoto = StationPhoto(null, path, photoSource)
            photos.add(stationPhoto)
            _onPhotos.add(stationPhoto)
            onPhotosNumber.postValue(photos.size)
        }
    }

    fun deletePhoto(photo: StationPhoto) {
        photo.remotePath?.let { photoRemotePath ->
            if (photos.firstOrNull { it.remotePath == photoRemotePath } != null) {
                viewModelScope.launch(dispatcher) {
                    onDeletingPhotoStatus.postValue(Resource.loading())
                    usecase.deleteDevicePhoto(device.id, photoRemotePath).onLeft {
                        Timber.e("Failed to delete photo $photo")
                        onDeletingPhotoStatus.postValue(Resource.error(it.getDefaultMessage()))
                    }.onRight {
                        onDeletingPhotoStatus.postValue(Resource.success(Unit))
                        onDeletedPhoto(photo)
                    }
                }
            }
        }
        photo.localPath?.let { photoLocalPath ->
            if (photos.firstOrNull { it.localPath == photoLocalPath } != null) {
                File(photoLocalPath).delete()
                onDeletedPhoto(photo)
            }
        }
    }

    private fun onDeletedPhoto(photo: StationPhoto) {
        photos.remove(photo)
        _onPhotos.remove(photo)
        onPhotosNumber.postValue(photos.size)
    }
}
