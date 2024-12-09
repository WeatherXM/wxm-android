package com.weatherxm.ui.photoverification.gallery

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice

class PhotoGalleryViewModel(
    val device: UIDevice,
    val photos: MutableList<StationPhoto>,
    val fromClaiming: Boolean
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
        if (photos.firstOrNull { it.remotePath == photo.remotePath } != null) {
            // TODO: Call the delete endpoint and post the new state on success
            photos.remove(photo)
            _onPhotos.remove(photo)
            onPhotosNumber.postValue(photos.size)
        } else if (photos.firstOrNull { it.localPath == photo.localPath } != null) {
            // TODO: Delete it actually from the storage saved 
            photos.remove(photo)
            _onPhotos.remove(photo)
            onPhotosNumber.postValue(photos.size)
        }
    }
}
