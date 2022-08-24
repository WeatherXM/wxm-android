package com.weatherxm.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.weatherxm.usecases.StartupUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StartupViewModel : ViewModel(), KoinComponent {
    private val startupUseCase: StartupUseCase by inject()

    fun startup() = startupUseCase.getStartupState().asLiveData()
}
