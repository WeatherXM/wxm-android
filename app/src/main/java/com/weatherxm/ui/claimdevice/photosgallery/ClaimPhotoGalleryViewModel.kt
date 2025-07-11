package com.weatherxm.ui.claimdevice.photosgallery

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.weatherxm.ui.common.PhotoSource
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.DevicePhotoUseCase
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File

class ClaimPhotoGalleryViewModel(
    val device: UIDevice,
    private val usecase: DevicePhotoUseCase,
    val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _onPhotos = mutableStateListOf<StationPhoto>()
    val onPhotos: List<StationPhoto> = _onPhotos

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
}
