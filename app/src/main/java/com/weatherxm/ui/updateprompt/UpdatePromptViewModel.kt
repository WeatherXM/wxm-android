package com.weatherxm.ui.updateprompt

import androidx.lifecycle.ViewModel
import com.weatherxm.usecases.UpdatePromptUseCase

class UpdatePromptViewModel(private val usecase: UpdatePromptUseCase) : ViewModel() {
    fun isUpdateMandatory(): Boolean {
        return usecase.isUpdateMandatory()
    }

    fun getChangelog(): String {
        return usecase.getChangelog()
    }
}
