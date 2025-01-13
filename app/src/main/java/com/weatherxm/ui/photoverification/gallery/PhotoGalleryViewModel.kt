package com.weatherxm.ui.photoverification.gallery

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.util.ImageFileHelper.getUriForFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File

class PhotoGalleryViewModel(
    val device: UIDevice,
    val photos: MutableList<StationPhoto>,
    val fromClaiming: Boolean,
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
        photo.remotePath?.let {  photoRemotePath ->
            // TODO: STOPSHIP: Call the delete endpoint and post the new state on success
            if (photos.firstOrNull { it.remotePath == photoRemotePath } != null) {
                onDeletedPhoto(photo)
            }
        }
        photo.localPath?.let {  photoLocalPath ->
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

    fun getUrisOfLocalPhotos(context: Context): ArrayList<Uri> {
        val uris = arrayListOf<Uri>()
        photos.forEach {
            if (!it.localPath.isNullOrEmpty()) {
                uris.add(File(it.localPath).getUriForFile(context))
            }
        }
        return uris
    }
}
