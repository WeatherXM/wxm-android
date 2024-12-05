package com.weatherxm.ui.photoverification.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PhotoGalleryViewModel(
    val currentPhotosList: List<String>,
    val fromClaiming: Boolean
) : ViewModel() {
    private val newPhotosTaken = mutableListOf<String>()

    private val onPhotoNumber = MutableLiveData(getPhotoNumber())

    fun onPhotoNumber(): LiveData<Int> = onPhotoNumber

    private fun getPhotoNumber() = newPhotosTaken.size + currentPhotosList.size

    fun addPhoto(path: String) {
        if (path.isNotEmpty() && !newPhotosTaken.contains(path)) {
            newPhotosTaken.add(path)
            onPhotoNumber.postValue(getPhotoNumber())
        }
    }

    fun deletePhoto(path: String) {
        if (currentPhotosList.contains(path)) {
            // TODO: Call the delete endpoint and post the new number on success
            onPhotoNumber.postValue(getPhotoNumber())
        } else if (newPhotosTaken.contains(path)) {
            newPhotosTaken.remove(path)
            onPhotoNumber.postValue(getPhotoNumber())
        }
    }
}
