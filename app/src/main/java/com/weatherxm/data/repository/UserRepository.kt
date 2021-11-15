package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.datasource.UserDataSource
import org.koin.core.component.KoinComponent

class UserRepository(
    private val userDataSource: UserDataSource
) : KoinComponent {

    suspend fun getUser(): Either<Failure, User> {
        return userDataSource.getUser()
    }

    suspend fun saveAddress(address: String): Either<Failure, Unit> {
        return userDataSource.saveAddress(address)
    }
}
