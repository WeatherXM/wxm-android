package com.weatherxm.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.weatherxm.usecases.StartupUseCase

class StartupViewModel(private val startupUseCase: StartupUseCase) : ViewModel() {

    fun startup() = startupUseCase.getStartupState().asLiveData()
}
