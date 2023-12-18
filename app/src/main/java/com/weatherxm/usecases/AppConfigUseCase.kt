package com.weatherxm.usecases

import com.weatherxm.data.repository.AppConfigRepository
import org.koin.core.component.KoinComponent

interface AppConfigUseCase {
    fun isTokenClaimingEnabled(): Boolean
    fun isPoLEnabled(): Boolean
}

class AppConfigUseCaseImpl(
    private val repository: AppConfigRepository
) : AppConfigUseCase, KoinComponent {
    override fun isTokenClaimingEnabled(): Boolean {
        return repository.isTokenClaimingEnabled()
    }

    override fun isPoLEnabled(): Boolean {
        return repository.isPoLEnabled()
    }
}
