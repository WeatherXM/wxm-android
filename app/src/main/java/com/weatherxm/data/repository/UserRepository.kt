package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User

interface UserRepository {
    suspend fun getUser(): Either<Failure, User>
    suspend fun getUserUsername(): Either<Failure, String>
    suspend fun deleteAccount(): Either<Failure, Unit>
    fun hasDismissedSurveyPrompt(): Boolean
    fun dismissSurveyPrompt()
    fun getUserId(): String
}
