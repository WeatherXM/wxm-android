package com.weatherxm.usecases

import com.weatherxm.data.repository.UserRepository

interface DeleteAccountSurveyUseCase {
    fun getUserId(): String
}

class DeleteAccountSurveyUseCaseImpl(
    private val userRepository: UserRepository
) : DeleteAccountSurveyUseCase {
    override fun getUserId(): String {
        return userRepository.getUserId()
    }
}
