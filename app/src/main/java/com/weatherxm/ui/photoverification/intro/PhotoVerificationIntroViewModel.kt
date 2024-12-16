package com.weatherxm.ui.photoverification.intro

import androidx.lifecycle.ViewModel
import com.weatherxm.usecases.DevicePhotoUseCase

class PhotoVerificationIntroViewModel(private val useCase: DevicePhotoUseCase) : ViewModel() {
    fun getAcceptedTerms(): Boolean = useCase.getAcceptedTerms()
    fun setAcceptedTerms() = useCase.setAcceptedTerms()
}
