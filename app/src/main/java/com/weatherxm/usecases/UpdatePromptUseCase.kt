package com.weatherxm.usecases

import com.weatherxm.data.repository.AppConfigRepository

interface UpdatePromptUseCase {
    fun getChangelog(): String
    fun isUpdateMandatory(): Boolean
}

class UpdatePromptUseCaseImpl(
    private val appConfigRepository: AppConfigRepository
) : UpdatePromptUseCase {
    override fun getChangelog(): String {
        return appConfigRepository.getChangelog()
    }

    override fun isUpdateMandatory(): Boolean {
        return appConfigRepository.isUpdateMandatory()
    }
}
