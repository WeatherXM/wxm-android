package com.weatherxm.ui.claimdevice.photosgallery

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.data.models.PhotoPresignedMetadata
import com.weatherxm.ui.common.PhotoSource
import com.weatherxm.ui.common.StationPhoto
import java.io.File

class ClaimPhotosGalleryViewModel : ViewModel() {
    private val _onRequestCameraPermission = MutableLiveData(false)
    private val _onPhotos = mutableStateListOf<StationPhoto>()
    private val onPhotosMetadata = MutableLiveData<List<PhotoPresignedMetadata>>()

    fun onRequestCameraPermission() = _onRequestCameraPermission
    val onPhotos: List<StationPhoto> = _onPhotos
    fun onPhotosMetadata(): LiveData<List<PhotoPresignedMetadata>> = onPhotosMetadata

    fun addPhoto(path: String, photoSource: PhotoSource) {
        if (path.isNotEmpty()) {
            val stationPhoto = StationPhoto(null, path, photoSource)
            _onPhotos.add(stationPhoto)
        }
    }

    fun deletePhoto(photo: StationPhoto) {
        photo.localPath?.let { photoLocalPath ->
            File(photoLocalPath).delete()
            _onPhotos.remove(photo)
        }
    }

    fun requestCameraPermission() {
        _onRequestCameraPermission.postValue(true)
    }
}
