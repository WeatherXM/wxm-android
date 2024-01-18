package com.weatherxm.usecases

import com.weatherxm.data.repository.UserRepository

interface SendFeedbackUseCase {
    fun getUserId(): String
}

class SendFeedbackUseCaseImpl(private val userRepository: UserRepository) : SendFeedbackUseCase {
    override fun getUserId(): String {
        return userRepository.getUserId()
    }
}
