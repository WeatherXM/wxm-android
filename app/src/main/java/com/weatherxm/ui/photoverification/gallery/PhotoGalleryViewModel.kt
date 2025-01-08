package com.weatherxm.ui.photoverification.gallery

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.DevicePhotoUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class PhotoGalleryViewModel(
    val device: UIDevice,
    val photos: MutableList<StationPhoto>,
    val fromClaiming: Boolean,
    private val usecase: DevicePhotoUseCase,
    val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val onPhotosNumber = MutableLiveData(photos.size)
    private val _onPhotos = mutableStateListOf<StationPhoto>().apply {
        addAll(photos)
    }
    val onPhotos: List<StationPhoto> = _onPhotos

    fun onPhotosNumber(): LiveData<Int> = onPhotosNumber

    fun addPhoto(path: String) {
        if (path.isNotEmpty() && photos.firstOrNull { it.localPath == path } == null) {
            val stationPhoto = StationPhoto(null, path)
            photos.add(stationPhoto)
            _onPhotos.add(stationPhoto)
            onPhotosNumber.postValue(photos.size)
        }
    }

    fun deletePhoto(photo: StationPhoto) {
        photo.remotePath?.let { photoRemotePath ->
            if (photos.firstOrNull { it.remotePath == photoRemotePath } != null) {
                viewModelScope.launch(dispatcher) {
                    usecase.deleteDevicePhoto(device.id, photoRemotePath).onLeft {
                        Timber.e("Failed to delete photo $photo")
                    }
                }
                onDeletedPhoto(photo)
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

    fun getPhotosLocalPaths(): ArrayList<String> {
        val photosLocalPaths = arrayListOf<String>()
        photos.forEach {
            if (!it.localPath.isNullOrEmpty()) {
                photosLocalPaths.add(it.localPath)
            }
        }
        return photosLocalPaths
    }
}
