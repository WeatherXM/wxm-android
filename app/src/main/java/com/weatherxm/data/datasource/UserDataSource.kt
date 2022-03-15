package com.weatherxm.data.datasource

import android.content.SharedPreferences
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.map
import com.weatherxm.data.network.AddressBody
import com.weatherxm.data.network.ApiService

interface UserDataSource {
    fun getName(): String?
    fun getWalletAddress(): String?
    fun getEmail(): String
    fun setWalletAddress(walletAddress: String?)
    fun setName(name: String?)
    fun setEmail(email: String)
    fun hasDataInCache(): Boolean
    suspend fun getUser(): Either<Failure, User>
    suspend fun saveAddress(address: String): Either<Failure, Unit>
    suspend fun clear()
}

class NetworkUserDataSource(
    private val apiService: ApiService,
    private val preferences: SharedPreferences
) : UserDataSource {
    override suspend fun getUser(): Either<Failure, User> {
        return apiService.getUser().map()
    }

    override suspend fun saveAddress(address: String): Either<Failure, Unit> {
        return apiService.saveAddress(AddressBody(address)).map()
    }

    override suspend fun clear() {
        preferences.edit().clear().apply()
    }

    override fun getName(): String? {
        /*
         * Method used by CacheUserDataSourceImpl
         * This should never run
         */
        return null
    }

    override fun getWalletAddress(): String? {
        /*
         * Method used by CacheUserDataSourceImpl
         * This should never run
         */
        return null
    }

    override fun getEmail(): String {
        /*
         * Method used by CacheUserDataSourceImpl
         * This should never run
         */
        return ""
    }

    override fun setWalletAddress(walletAddress: String?) {
        /*
         * Method used by CacheUserDataSourceImpl
         * This should never run
         */
    }

    override fun setName(name: String?) {
        /*
         * Method used by CacheUserDataSourceImpl
         * This should never run
         */
    }

    override fun setEmail(email: String) {
        /*
         * Method used by CacheUserDataSourceImpl
         * This should never run
         */
    }

    override fun hasDataInCache(): Boolean {
        /*
         * Method used by CacheUserDataSourceImpl
         * This should never run
         */
        return false
    }
}

class CacheUserDataSource : UserDataSource {
    private var email: String = ""
    private var name: String? = null
    private var walletAddress: String? = null

    override fun hasDataInCache(): Boolean {
        return email.isNotEmpty()
    }

    override fun setEmail(email: String) {
        this.email = email
    }

    override fun setName(name: String?) {
        this.name = name
    }

    override fun setWalletAddress(walletAddress: String?) {
        this.walletAddress = walletAddress
    }

    override fun getEmail(): String {
        return email
    }

    override fun getName(): String? {
        return name
    }

    override fun getWalletAddress(): String? {
        return walletAddress
    }

    override suspend fun getUser(): Either<Failure, User> {
        /*
         * Method used by CacheUserDataSourceImpl
         * This should never run
         */
        return Either.Left(Failure.UnknownError)
    }

    override suspend fun saveAddress(address: String): Either<Failure, Unit> {
        /*
         * Method used by NetworkUserDataSourceImpl
         * This should never run
         */
        return Either.Left(Failure.UnknownError)
    }

    override suspend fun clear() {
        /*
         * Method used by NetworkUserDataSourceImpl
         * This should never run
         */
    }
}
