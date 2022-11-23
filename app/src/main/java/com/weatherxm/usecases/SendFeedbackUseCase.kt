package com.weatherxm.usecases

import com.weatherxm.data.network.interceptor.ClientIdentificationRequestInterceptor
import com.weatherxm.data.repository.UserRepository

interface SendFeedbackUseCase {
    fun getUserId(): String
    fun getClientIdentifier(): String
}

class SendFeedbackUseCaseImpl(
    private val userRepository: UserRepository,
    private val clientIdentificationRequestInterceptor: ClientIdentificationRequestInterceptor
) : SendFeedbackUseCase {

    override fun getUserId(): String {
        return userRepository.getUserId()
    }

    override fun getClientIdentifier(): String {
        return clientIdentificationRequestInterceptor.getClientIdentifier()
    }
}
