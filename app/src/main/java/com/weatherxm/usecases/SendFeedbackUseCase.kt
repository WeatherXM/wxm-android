package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.network.interceptor.ClientIdentificationRequestInterceptor
import com.weatherxm.data.repository.UserRepository

interface SendFeedbackUseCase {
    suspend fun getUser(): Either<Failure, User>
    fun getClientIdentifier(): String
}

class SendFeedbackUseCaseImpl(
    private val userRepository: UserRepository,
    private val clientIdentificationRequestInterceptor: ClientIdentificationRequestInterceptor
) : SendFeedbackUseCase {

    override suspend fun getUser(): Either<Failure, User> {
        return userRepository.getUser()
    }

    override fun getClientIdentifier(): String {
        return clientIdentificationRequestInterceptor.getClientIdentifier()
    }
}
