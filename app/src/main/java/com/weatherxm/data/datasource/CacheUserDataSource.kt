package com.weatherxm.data.datasource

import org.koin.core.component.KoinComponent

interface CacheUserDataSource {
    fun getName(): String?
    fun getWalletAddress(): String?
    fun getEmail(): String
    fun setWalletAddress(walletAddress: String?)
    fun setName(name: String?)
    fun setEmail(email: String)
    fun hasDataInCache(): Boolean
}

class CacheUserDataSourceImpl : CacheUserDataSource, KoinComponent {
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
}
