package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.mapResponse
import com.weatherxm.data.network.ApiService

class NetworkUserDataSource(private val apiService: ApiService) : UserDataSource {
    override suspend fun getUser(): Either<Failure, User> {
        return apiService.getUser().mapResponse()
    }

    override suspend fun deleteAccount(): Either<Failure, Unit> {
        return apiService.deleteAccount().mapResponse()
    }

    override suspend fun getUserUsername(): Either<Failure, String> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun setUserUsername(username: String) {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun setUser(user: User) {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override fun getUserId(): String {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }
}
