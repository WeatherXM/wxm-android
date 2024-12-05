package com.weatherxm.ui.photoverification.gallery

import androidx.lifecycle.ViewModel

class PhotoGalleryViewModel(
    val currentPhotosList: List<String>,
    val fromClaiming: Boolean
) : ViewModel() {
    private val newPhotosTaken = mutableListOf<String>()
}
