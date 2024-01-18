package com.weatherxm.ui.updateprompt

import androidx.lifecycle.ViewModel
import com.weatherxm.data.repository.AppConfigRepository

class UpdatePromptViewModel(private val appConfigRepository: AppConfigRepository) : ViewModel() {
    fun isUpdateMandatory(): Boolean {
        return appConfigRepository.isUpdateMandatory()
    }

    fun getChangelog(): String {
        return appConfigRepository.getChangelog()
    }
}
